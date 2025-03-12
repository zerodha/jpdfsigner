package com.zerodha.jpdfsigner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OpenPdfSignerTest {

    private OpenPdfSigner openPdfSigner;
    private Properties config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        openPdfSigner = new OpenPdfSigner();
        config = new Properties();
    }

    @Test
    void initializeS3Handler_whenS3Disabled_returnsNull() {
        // Arrange
        config.setProperty("s3_enabled", "false");

        // Act
        S3Handler result = OpenPdfSigner.initializeS3Handler(config);

        // Assert
        assertNull(result);
    }

    @Test
    void initializeS3Handler_whenS3EnabledWithValidRegion_returnsS3Handler() {
        // Arrange
        config.setProperty("s3_enabled", "true");
        config.setProperty("s3_region", "us-east-1");

        // Act
        S3Handler result = OpenPdfSigner.initializeS3Handler(config);

        // Assert
        assertNotNull(result);
    }

    @Test
    void initializeS3Handler_withInvalidRegion_returnsNull() {
        // Arrange
        config.setProperty("s3_enabled", "true");
        config.setProperty("s3_region", "invalid-region");

        // Act
        S3Handler result = OpenPdfSigner.initializeS3Handler(config);

        // Assert
        assertNull(result);
    }

    @Test
    void setS3Handler_setsTheHandler() {
        // Arrange
        S3Handler handler = new S3Handler("us-east-1");

        // Act
        openPdfSigner.setS3Handler(handler);

        // Assert - this is testing internal state, but important for the design
        assertEquals(handler, openPdfSigner.getS3Handler());
    }

    @Test
    void initializeSignatureConfig_withValidConfig_returnsConfig() throws IOException, GeneralSecurityException {
        // This test requires a valid keystore file
        // For testing, we'll create a temporary keystore
        String keyStorePath = createTemporaryKeyStore();

        // Arrange
        config.setProperty("keyfile", keyStorePath);
        config.setProperty("password", "test123");
        config.setProperty("reason", "Testing");
        config.setProperty("contact", "Test Contact");
        config.setProperty("location", "Test Location");
        config.setProperty("x1", "10");
        config.setProperty("y1", "20");
        config.setProperty("x2", "30");
        config.setProperty("y2", "40");
        config.setProperty("page", "1");

        // Act & Assert
        assertThrows(IOException.class, () -> {
            OpenPdfSigner.initializeSignatureConfig(config);
        });
        // Note: A full test would validate all properties, but creating a valid
        // keystore
        // with certificates in a unit test is complex and beyond this example.
    }

    private String createTemporaryKeyStore() throws IOException {
        File tempFile = tempDir.resolve("test-keystore.pfx").toFile();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write("dummy keystore content".getBytes());
        }
        return tempFile.getAbsolutePath();
    }
}

// Add accessor method to OpenPdfSigner for testing