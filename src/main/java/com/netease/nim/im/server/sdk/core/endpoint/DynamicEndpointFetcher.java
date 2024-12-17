package com.netease.nim.im.server.sdk.core.endpoint;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.netease.nim.im.server.sdk.core.Constants;
import com.netease.nim.im.server.sdk.core.exception.EndpointFetchException;
import com.netease.nim.im.server.sdk.core.http.ParamBuilder;
import com.netease.nim.im.server.sdk.core.utils.NamedThreadFactory;
import com.netease.nim.im.server.sdk.core.version.YunxinApiSdkVersion;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Created by caojiajun on 2024/12/9
 */
public class DynamicEndpointFetcher implements EndpointFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DynamicEndpointFetcher.class);

    private final String appkey;
    private final List<String> lbsList;
    private final int reloadIntervalSeconds;

    private OkHttpClient okHttpClient;

    private String md5;
    private Endpoints endpoints;
    private long nextFetchTime;

    public DynamicEndpointFetcher(String appkey) {
        this(appkey, null);
    }

    public DynamicEndpointFetcher(String appkey, Region region) {
        this.appkey = appkey;
        if (region == Region.CN) {
            this.lbsList = Collections.singletonList(Constants.Endpoint.LBS.cn_lbs);
        } else if (region == Region.SG) {
            this.lbsList = Collections.singletonList(Constants.Endpoint.LBS.sg_lbs);
        } else {
            this.lbsList = Arrays.asList(Constants.Endpoint.LBS.default_lbs, Constants.Endpoint.LBS.cn_lbs, Constants.Endpoint.LBS.sg_lbs);
        }
        this.reloadIntervalSeconds = Constants.Endpoint.scheduleFetchIntervalSeconds;
    }

    public DynamicEndpointFetcher(String appkey, List<String> lbsList, int reloadIntervalSeconds) {
        this.appkey = appkey;
        this.lbsList = lbsList;
        this.reloadIntervalSeconds = reloadIntervalSeconds;
    }

    @Override
    public void init(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        for (int i=0; i<3; i++) {//初始化时多试几次
            for (String lbs : lbsList) {
                try {
                    boolean reload = reload(lbs);
                    if (reload) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error("fetch endpoints error, lbs = {}", lbs, e);
                }
            }
            if (endpoints != null) {
                break;
            }
        }
        if (endpoints == null) {
            throw new EndpointFetchException("init endpoints error");
        }
        Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("yunxin-im-sdk-endpoint-fetch"))
                .scheduleAtFixedRate(() -> {
                    for (String lbs : lbsList) {
                        try {
                            boolean reload = reload(lbs);
                            if (reload) {
                                break;
                            }
                        } catch (Exception e) {
                            logger.error("fetch endpoints error, lbs = {}", lbs, e);
                        }
                    }
                }, reloadIntervalSeconds, reloadIntervalSeconds, TimeUnit.SECONDS);
    }

    private boolean reload(String lbs) {
        if (System.currentTimeMillis() < nextFetchTime) {
            return true;
        }
        ParamBuilder builder = new ParamBuilder();
        builder.addParam("k", appkey);
        builder.addParam("sv", YunxinApiSdkVersion.version);
        if (md5 != null) {
            builder.addParam("md5", md5);
        }
        Request request = new Request.Builder().get()
                .url(lbs + "?" + builder.build())
                .build();
        String string = null;
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.code() != 200) {
                logger.error("fetch endpoints error, http.code = {}", response.code());
                throw new EndpointFetchException("http.code=" + response.code());
            }
            string = response.body().string();
            if (logger.isDebugEnabled()) {
                logger.debug("fetch endpoints, lbs = {}, response = {}", lbs, string);
            }
            JSONObject json = JSONObject.parseObject(string);
            Integer code = json.getInteger("code");
            if (code == null) {
                logger.error("illegal endpoints, response = {}", string);
                return false;
            }
            if (code == 304) {//没有发生变更
                return true;
            }
            if (code == 200) {
                JSONObject data = json.getJSONObject("data");
                String defaultEndpoint = data.getString("default.endpoint");
                if (!check(defaultEndpoint)) {
                    logger.error("default endpoint check error, skip update, endpoint = {}", defaultEndpoint);
                    return false;
                }
                JSONArray backupEndpointsJson = data.getJSONArray("backup.endpoints");
                Endpoints endpoints = new Endpoints();
                List<String> backupEndpoints = new ArrayList<>();
                boolean existsSkip = false;
                if (backupEndpointsJson != null) {
                    for (Object backupEndpoint : backupEndpointsJson) {
                        if (!check(String.valueOf(backupEndpoint))) {
                            logger.error("backup endpoint check error, skip, endpoint = {}", defaultEndpoint);
                            existsSkip = true;
                            continue;
                        }
                        backupEndpoints.add(String.valueOf(backupEndpoint));
                    }
                }
                endpoints.setDefaultEndpoint(defaultEndpoint);
                endpoints.setBackupEndpoints(backupEndpoints);
                if (defaultEndpoint == null) {
                    logger.error("illegal endpoints, response = {}", string);
                    return false;
                }
                logger.info("endpoints update, old = {}, new = {}", JSONObject.toJSONString(this.endpoints), JSONObject.toJSONString(endpoints));
                int ttl = data.getIntValue("ttl", 30);
                if (ttl <= 0 || ttl > 86400) {
                    ttl = 30;
                }
                this.nextFetchTime = System.currentTimeMillis() + ttl * 1000L;
                if (!existsSkip) {
                    this.md5 = data.getString("md5");
                }
                this.endpoints = endpoints;
                return true;
            } else {
                logger.error("fetch endpoints error, response = {}", string);
                return false;
            }
        } catch (EndpointFetchException e) {
            throw e;
        } catch (Exception e) {
            logger.error("fetch endpoints error, response = {}", string, e);
            throw new EndpointFetchException(e);
        }
    }

    @Override
    public Endpoints get() {
        return endpoints;
    }

    private boolean check(String endpoint) {
        if (okHttpClient == null) {
            return true;
        }
        String url = endpoint + Constants.Endpoint.detectPath;
        Request request = new Request.Builder().get()
                .url(url)
                .build();
        boolean success;
        try (Response response = okHttpClient.newCall(request).execute()) {
            String string = response.body().string();
            success = response.code() == 200;
            if (logger.isDebugEnabled()) {
                logger.debug("check, endpoint = {}, path = {}, code = {}, response = {}", endpoint, Constants.Endpoint.detectPath, response.code(), string);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("check error, endpoint = {}, path = {}", endpoint, Constants.Endpoint.detectPath, e);
            }
            success = false;
        }
        return success;
    }
}
