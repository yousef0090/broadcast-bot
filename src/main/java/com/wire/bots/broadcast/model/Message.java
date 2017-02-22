package com.wire.bots.broadcast.model;

/**
 * Created with IntelliJ IDEA.
 * User: dejankovacevic
 * Date: 21/02/17
 * Time: 11:50
 */
public class Message {
    private String botId;
    private String userId;
    private String text;
    private String assetKey;
    private String token;
    private byte[] otrKey;
    private String mimeType;
    private long size;
    private long time;
    private byte[] sha256;

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public void setAssetKey(String assertKey) {
        this.assetKey = assertKey;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public byte[] getOtrKey() {
        return otrKey;
    }

    public void setOtrKey(byte[] otrKey) {
        this.otrKey = otrKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public byte[] getSha256() {
        return sha256;
    }

    public void setSha256(byte[] sha256) {
        this.sha256 = sha256;
    }
}
