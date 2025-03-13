package com.zerodha.jpdfsigner;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;

public class SignParams {

    private String src;
    private String dest;
    private String reason;
    private String location;
    private String contact;
    private String password;
    private Certificate[] chain;
    private PrivateKey key;
    private Rectangle rect;
    private Font font;
    private int page;

    // Constructor
    public SignParams() {}

    // Getters and Setters
    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Certificate[] getChain() {
        return chain != null ? Arrays.copyOf(chain, chain.length) : null;
    }

    public void setChain(Certificate[] chain) {
        this.chain = chain != null ? Arrays.copyOf(chain, chain.length) : null;
    }

    public PrivateKey getKey() {
        return key;
    }

    public void setKey(PrivateKey key) {
        this.key = key;
    }

    public Rectangle getRect() {
        return rect;
    }

    public void setRect(Rectangle rect) {
        this.rect = rect;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
