package com.zerodha.jpdfsigner;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class SignatureConfigTest {

    @Test
    void constructor_setsAllProperties() {
        // Arrange
        Font font = mock(Font.class);
        String reason = "Test Reason";
        String contact = "Test Contact";
        String location = "Test Location";
        Rectangle rect = new Rectangle(10, 20, 30, 40);
        int page = 1;
        PrivateKey key = mock(PrivateKey.class);
        Certificate[] chain = new Certificate[] { mock(Certificate.class) };

        // Act
        SignatureConfig config = new SignatureConfig(font, reason, contact, location, rect, page, key, chain);

        // Assert
        assertSame(font, config.getFont());
        assertEquals(reason, config.getReason());
        assertEquals(contact, config.getContact());
        assertEquals(location, config.getLocation());
        assertSame(rect, config.getRect());
        assertEquals(page, config.getPage());
        assertSame(key, config.getKey());
        assertSame(chain, config.getChain());
    }
}