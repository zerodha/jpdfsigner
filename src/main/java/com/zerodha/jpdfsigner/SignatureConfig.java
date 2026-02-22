package com.zerodha.jpdfsigner;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 * Holds signature configuration for PDF signing
 */
@Getter
@AllArgsConstructor
public class SignatureConfig {
    private final Font font;
    private final String reason;
    private final String contact;
    private final String location;
    private final Rectangle rect;
    private final int page;
    private final PrivateKey key;
    private final Certificate[] chain;
}
