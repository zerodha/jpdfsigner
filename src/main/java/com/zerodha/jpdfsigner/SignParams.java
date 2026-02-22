package com.zerodha.jpdfsigner;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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

    public Certificate[] getChain() {
        return chain != null ? Arrays.copyOf(chain, chain.length) : null;
    }

    public void setChain(Certificate[] chain) {
        this.chain = chain != null ? Arrays.copyOf(chain, chain.length) : null;
    }
}
