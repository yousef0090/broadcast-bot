package com.wire.bots.broadcast.model;

/**
 * Created with IntelliJ IDEA.
 * User: dejankovacevic
 * Date: 21/02/17
 * Time: 11:50
 */
public class Broadcast {
    private int id;
    private String text;
    private long time;
    private String assetKey;
    private String token;
    private byte[] otrKey;
    private byte[] sha256;
    private String mimeType;
    private long size;
    private String url;
    private String title;
    private byte[] assetData;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setAssetKey(String assetKey) {
        this.assetKey = assetKey;
    }

    public String getAssetKey() {
        return assetKey;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setOtrKey(byte[] otrKey) {
        this.otrKey = otrKey;
    }

    public byte[] getOtrKey() {
        return otrKey;
    }

    public void setSha256(byte[] sha256) {
        this.sha256 = sha256;
    }

    public byte[] getSha256() {
        return sha256;
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

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public byte[] getAssetData() {
        return assetData;
    }

    public void setAssetData(byte[] assetData) {
        this.assetData = assetData;
    }
}
