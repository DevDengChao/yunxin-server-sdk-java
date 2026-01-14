package com.netease.nim.server.sdk.rtc.room.request;
/**
 * Created by DevDengChao on 2025/01/14
 *
 * See https://doc.yunxin.163.com/nertc/server-apis/TM3MzM4MzM?platform=server
 */
public class RtcSetMemberBanStatusRequestV2 {
    private Long cid;
    private Number uid;
    private Boolean isBanned;

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

    public Number getUid() {
        return uid;
    }

    public void setUid(Number uid) {
        this.uid = uid;
    }

    public Boolean getIsBanned() {
        return isBanned;
    }

    public void setIsBanned(Boolean isBanned) {
        this.isBanned = isBanned;
    }
}
