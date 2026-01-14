package com.netease.nim.server.sdk.rtc.room.request;
/**
 * Created by DevDengChao on 2025/01/14
 *
 * See https://doc.yunxin.163.com/nertc/server-apis/TM3MzM4MzM?platform=server
 */
public class RtcSetMemberBanStatusRequestV3 {
    private String cname;
    private Number uid;
    private Boolean isBanned;

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
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
