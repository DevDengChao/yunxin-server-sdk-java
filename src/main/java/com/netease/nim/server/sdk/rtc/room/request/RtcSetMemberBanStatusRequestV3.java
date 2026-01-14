package com.netease.nim.server.sdk.rtc.room.request;
/**
 * Created by DevDengChao on 2025/01/14
 *
 * See https://doc.yunxin.163.com/nertc/server-apis/TM3MzM4MzM?platform=server
 */
public class RtcSetMemberBanStatusRequestV3 {
    private String cname;
    private Number uid;
    private Number audioRight;
    private Number audioDuration;
    private Number videoRight;
    private Number videoDuration;

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

    public Number getAudioRight() {
        return audioRight;
    }

    public void setAudioRight(Number audioRight) {
        this.audioRight = audioRight;
    }

    public Number getAudioDuration() {
        return audioDuration;
    }

    public void setAudioDuration(Number audioDuration) {
        this.audioDuration = audioDuration;
    }

    public Number getVideoRight() {
        return videoRight;
    }

    public void setVideoRight(Number videoRight) {
        this.videoRight = videoRight;
    }

    public Number getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(Number videoDuration) {
        this.videoDuration = videoDuration;
    }
}
