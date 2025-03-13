package com.zerodha.jpdfsigner;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Holds signature configuration for PDF signing
 */
public class SignatureConfig {
    private final Font font;
    private final String reason;
    private final String contact;
    private final String location;
    private final Rectangle rect;
    private final int page;
    private final PrivateKey key;
    private final Certificate[] chain;

    public SignatureConfig(Font font, String reason, String contact, String location,
            Rectangle rect, int page, PrivateKey key, Certificate[] chain) {
        this.font = font;
        this.reason = reason;
        this.contact = contact;
        this.location = location;
        this.rect = rect;
        this.page = page;
        this.key = key;
        this.chain = chain;
    }

    public Font getFont() {
        return font;
    }

    public String getReason() {
        return reason;
    }

    public String getContact() {
        return contact;
    }

    public String getLocation() {
        return location;
    }

    public Rectangle getRect() {
        return rect;
    }

    public int getPage() {
        return page;
    }

    public PrivateKey getKey() {
        return key;
    }

    public Certificate[] getChain() {
        return chain;
    }
}