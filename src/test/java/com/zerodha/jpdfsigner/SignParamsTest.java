package com.zerodha.jpdfsigner;

import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class SignParamsTest {

    private SignParams signParams;

    @BeforeEach
    void setUp() {
        signParams = new SignParams();
    }

    @Test
    void setSrc_setsSrcAttribute() {
        // Arrange
        String src = "/path/to/source.pdf";

        // Act
        signParams.setSrc(src);

        // Assert
        assertEquals(src, signParams.getSrc());
    }

    @Test
    void setDest_setsDestAttribute() {
        // Arrange
        String dest = "/path/to/destination.pdf";

        // Act
        signParams.setDest(dest);

        // Assert
        assertEquals(dest, signParams.getDest());
    }

    @Test
    void setPassword_setsPasswordAttribute() {
        // Arrange
        String password = "testPassword";

        // Act
        signParams.setPassword(password);

        // Assert
        assertEquals(password, signParams.getPassword());
    }

    @Test
    void setReason_setsReasonAttribute() {
        // Arrange
        String reason = "Test Reason";

        // Act
        signParams.setReason(reason);

        // Assert
        assertEquals(reason, signParams.getReason());
    }

    @Test
    void setContact_setsContactAttribute() {
        // Arrange
        String contact = "Test Contact";

        // Act
        signParams.setContact(contact);

        // Assert
        assertEquals(contact, signParams.getContact());
    }

    @Test
    void setLocation_setsLocationAttribute() {
        // Arrange
        String location = "Test Location";

        // Act
        signParams.setLocation(location);

        // Assert
        assertEquals(location, signParams.getLocation());
    }

    @Test
    void setKey_setsKeyAttribute() {
        // Arrange
        PrivateKey key = mock(PrivateKey.class);

        // Act
        signParams.setKey(key);

        // Assert
        assertSame(key, signParams.getKey());
    }

    @Test
    void setChain_setsChainAttribute() {
        // Arrange
        Certificate[] chain = new Certificate[] { mock(Certificate.class) };

        // Act
        signParams.setChain(chain);

        // Assert
        // Use assertEquals instead of assertSame for arrays
        assertEquals(chain[0], signParams.getChain()[0]);
    }

    @Test
    void setRect_setsRectAttribute() {
        // Arrange
        Rectangle rect = new Rectangle(10, 20, 30, 40);

        // Act
        signParams.setRect(rect);

        // Assert
        assertSame(rect, signParams.getRect());
    }

    @Test
    void setFont_setsFontAttribute() {
        // Arrange
        Font font = mock(Font.class);

        // Act
        signParams.setFont(font);

        // Assert
        assertSame(font, signParams.getFont());
    }

    @Test
    void setPage_setsPageAttribute() {
        // Arrange
        int page = 5;

        // Act
        signParams.setPage(page);

        // Assert
        assertEquals(page, signParams.getPage());
    }
}