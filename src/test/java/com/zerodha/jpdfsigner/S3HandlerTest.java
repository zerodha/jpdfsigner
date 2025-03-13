package com.zerodha.jpdfsigner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3HandlerTest {

    @Mock
    private S3Client s3Client;

    private S3Handler s3Handler;

    @BeforeEach
    void setUp() throws Exception {
        // Use reflection to inject our mock S3Client into the S3Handler
        s3Handler = new S3Handler("us-east-1") {
            @Override
            protected S3Client createS3Client(String region) {
                return s3Client;
            }
        };
    }

    @Test
    void isS3Path_withS3Prefix_returnsTrue() {
        assertTrue(S3Handler.isS3Path("s3://bucket/object.pdf"));
    }

    @Test
    void isS3Path_withoutS3Prefix_returnsFalse() {
        assertFalse(S3Handler.isS3Path("/local/path/file.pdf"));
    }

    @Test
    void isS3Path_withNullPath_returnsFalse() {
        assertFalse(S3Handler.isS3Path(null));
    }

    @Test
    void getInputStreamFromS3_validPath_returnsInputStream() throws IOException {
        // Arrange
        String s3Path = "s3://test-bucket/test-file.pdf";
        byte[] testData = "Test PDF Content".getBytes();

        // Create a mock ResponseInputStream
        ResponseInputStream<GetObjectResponse> mockResponse = spy(
                new ResponseInputStream<>(GetObjectResponse.builder().build(),
                        new ByteArrayInputStream(testData)));

        // Mock the S3 client behavior
        when(s3Client.getObject((GetObjectRequest) any())).thenReturn(mockResponse);

        // Act
        InputStream result = s3Handler.getInputStreamFromS3(s3Path);

        // Assert
        assertNotNull(result);

        // Verify the correct request was made
        verify(s3Client).getObject((GetObjectRequest) argThat(request -> request instanceof GetObjectRequest &&
                "test-bucket".equals(((GetObjectRequest) request).bucket()) &&
                "test-file.pdf".equals(((GetObjectRequest) request).key())));

        // Check content is correct
        byte[] buffer = new byte[testData.length];
        result.read(buffer);
        assertArrayEquals(testData, buffer);
    }

    @Test
    void getInputStreamFromS3_invalidPath_throwsException() {
        // Arrange
        String invalidPath = "invalid-path";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            s3Handler.getInputStreamFromS3(invalidPath);
        });
    }

    @Test
    void getInputStreamFromS3_clientError_rethrowsException() {
        // Arrange
        AwsServiceException s3Exception = S3Exception.builder().message("S3 Error").build();
        when(s3Client.getObject((GetObjectRequest) any())).thenThrow(s3Exception);

        // Act & Assert
        assertThrows(AwsServiceException.class, () -> {
            s3Handler.getInputStreamFromS3("s3://bucket/object.pdf");
        });
    }

    @Test
    void uploadToS3_validInput_uploadsSuccessfully() {
        // Arrange
        String s3Path = "s3://test-bucket/output.pdf";
        byte[] testData = "Test Output PDF".getBytes();
        InputStream inputStream = new ByteArrayInputStream(testData);
        long dataLength = testData.length;

        // Mock successful upload with a null return since the method returns
        // PutObjectResponse
        when(s3Client.putObject((PutObjectRequest) any(), any(RequestBody.class))).thenReturn(null);

        // Act - no exception should be thrown
        assertDoesNotThrow(() -> {
            s3Handler.uploadToS3(inputStream, dataLength, s3Path);
        });

        // Verify the correct request was made
        verify(s3Client).putObject(
                (PutObjectRequest) argThat(request -> request instanceof PutObjectRequest &&
                        "test-bucket".equals(((PutObjectRequest) request).bucket()) &&
                        "output.pdf".equals(((PutObjectRequest) request).key()) &&
                        ((PutObjectRequest) request).contentType().equals("application/pdf")),
                any(RequestBody.class));
    }

    @Test
    void uploadToS3_nullInputStream_throwsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            s3Handler.uploadToS3(null, 10, "s3://bucket/object.pdf");
        });
    }

    @Test
    void uploadToS3_nullPath_throwsException() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            s3Handler.uploadToS3(inputStream, 4, null);
        });
    }

    @Test
    void uploadToS3_clientError_rethrowsException() {
        // Arrange
        ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());
        AwsServiceException s3Exception = S3Exception.builder().message("S3 Error").build();
        doThrow(s3Exception).when(s3Client).putObject((PutObjectRequest) any(), any(RequestBody.class));

        // Act & Assert
        assertThrows(AwsServiceException.class, () -> {
            s3Handler.uploadToS3(inputStream, 4, "s3://bucket/object.pdf");
        });
    }

    @Test
    void close_closesResources() {
        // Act
        s3Handler.close();

        // Verify
        verify(s3Client).close();
    }
}