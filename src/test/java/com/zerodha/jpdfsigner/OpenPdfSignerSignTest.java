package com.zerodha.jpdfsigner;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenPdfSignerSignTest {

    @Mock
    private S3Handler s3Handler;

    @Mock
    private PrivateKey privateKey;

    // Can't mock arrays directly with Mockito
    private Certificate[] certificateChain;

    @Mock
    private Font font;

    @TempDir
    Path tempDir;

    private OpenPdfSigner openPdfSigner;
    private SignParams signParams;
    private Properties config;

    @BeforeEach
    void setUp() {
        openPdfSigner = new OpenPdfSigner();
        signParams = new SignParams();
        config = new Properties();

        config.setProperty("s3_enabled", "true");
        config.setProperty("s3_region", "us-east-1");

        // Act
        S3Handler s3Handler = OpenPdfSigner.initializeS3Handler(config);

        this.s3Handler = s3Handler;
    

        // Create a certificate array with a mock certificate
        Certificate mockCert = mock(Certificate.class);
        certificateChain = new Certificate[] { mockCert };

        // Setup signParams with required attributes
        signParams.setKey(privateKey);
        signParams.setChain(certificateChain);
        signParams.setReason("Test Reason");
        signParams.setContact("Test Contact");
        signParams.setLocation("Test Location");
        signParams.setFont(font);
        signParams.setRect(new Rectangle(0, 0, 100, 100));
        signParams.setPage(1);
    }

    @Test
    void sign_withS3PathsButNoHandler_throwsException() {
        // Arrange
        signParams.setSrc("s3://bucket/input.pdf");
        signParams.setDest("s3://bucket/output.pdf");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            openPdfSigner.sign(signParams);
        });
    }

    // @Test
    // void sign_withS3SourceAndS3Handler_usesS3Handler() throws IOException, DocumentException {
    //     // Arrange
    //     signParams.setSrc("s3://bucket/input.pdf");
    //     signParams.setDest(tempDir.resolve("output.pdf").toString());

    //     // Create a minimal PDF content for testing
    //     byte[] pdfContent = createMinimalPdfContent();

    //     // Setup mock behavior
    //     openPdfSigner.setS3Handler(s3Handler);
    //     when(s3Handler.getInputStreamFromS3(eq("s3://bucket/input.pdf")))
    //             .thenReturn(new ByteArrayInputStream(pdfContent));

    //     // Act & Assert - since we can't easily test the actual signing process
    //     // we'll just verify that it attempts to use the S3Handler
    //     assertThrows(Exception.class, () -> {
    //         openPdfSigner.sign(signParams);
    //     });

    //     // Verify the S3Handler was used
    //     verify(s3Handler).getInputStreamFromS3(eq("s3://bucket/input.pdf"));
    // }

    // @Test
    // void sign_withS3DestinationAndS3Handler_usesS3Handler() throws IOException, DocumentException {
    //     // Arrange
    //     Path testPdfPath = createTestPdfFile();
    //     signParams.setSrc(testPdfPath.toString());
    //     signParams.setDest("s3://bucket/output.pdf");

    //     openPdfSigner.setS3Handler(s3Handler);

    //     // Act & Assert
    //     assertThrows(Exception.class, () -> {
    //         openPdfSigner.sign(signParams);
    //     });

    //     // Verify that uploadToS3 is called if necessary
    //     verify(s3Handler, times(1)).uploadToS3(any(), anyLong(), eq("s3://bucket/output.pdf"));
    // }

    private byte[] createMinimalPdfContent() {
        // This is not a valid PDF, just something to test with
        return "%PDF-1.4\n1 0 obj\n<</Type/Catalog/Pages 2 0 R>>\nendobj\n%EOF".getBytes();
    }

    private Path createTestPdfFile() throws IOException {
        Path pdfPath = tempDir.resolve("test.pdf");
        Files.write(pdfPath, createMinimalPdfContent());
        return pdfPath;
    }
}