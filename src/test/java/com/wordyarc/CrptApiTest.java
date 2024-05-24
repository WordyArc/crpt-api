package com.wordyarc;

import java.io.*;
import java.util.concurrent.*;

import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.*;
import org.apache.hc.core5.http.io.entity.*;
import org.mockito.*;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class CrptApiTest {
    private CloseableHttpClient mockHttpClient;
    private CloseableHttpResponse mockResponse;
    private CrptApi api;

    @BeforeEach
    public void setUp() {
        mockHttpClient = Mockito.mock(CloseableHttpClient.class);
        mockResponse = Mockito.mock(CloseableHttpResponse.class);
        api = new CrptApi(TimeUnit.SECONDS, 2);

        try {
            var field = CrptApi.class.getDeclaredField("httpClient");
            field.setAccessible(true);
            field.set(api, mockHttpClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreateDocumentSuccess() throws Exception {
        when(mockResponse.getCode()).thenReturn(200);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"result\":\"success\"}", ContentType.APPLICATION_JSON));
        when(mockHttpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenReturn("Response: {\"result\":\"success\"}");

        CrptApi.Document doc = createSampleDocument();
        String signature = "testSignature";

        api.createDocument(doc, signature);
        api.dispose();

        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(mockHttpClient).execute(captor.capture(), any(HttpClientResponseHandler.class));
        assertEquals("https://ismp.crpt.ru/api/v3/lk/documents/create", captor.getValue().getUri().toString());
    }

    @Test
    public void testCreateDocumentRateLimit() throws InterruptedException, IOException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 2);

        Runnable task = () -> {
            try {
                CrptApi.Document doc = createSampleDocument();
                String signature = "testSignature";
                api.createDocument(doc, signature);
            } catch (InterruptedException | IOException e) {
                fail("Исключение не должно быть выброшено: " + e.getMessage());
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
        executor.schedule(task, 0, TimeUnit.SECONDS);
        executor.schedule(task, 0, TimeUnit.SECONDS);
        executor.schedule(task, 1, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    public void testCreateDocumentErrorHandling() throws Exception {
        when(mockResponse.getCode()).thenReturn(401);
        when(mockResponse.getEntity()).thenReturn(new StringEntity("{\"error\":\"unauthorized\"}", ContentType.APPLICATION_JSON));
        when(mockHttpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenThrow(new IOException("Error: 401 Body: {\"error\":\"unauthorized\"}"));

        CrptApi.Document doc = createSampleDocument();
        String signature = "testSignature";
        Exception exception = assertThrows(IOException.class, () -> {
            api.createDocument(doc, signature);
        });

        assertTrue(exception.getMessage().contains("Error: 401 Body: {\"error\":\"unauthorized\"}"));
    }

    private CrptApi.Document createSampleDocument() {
        CrptApi.Document doc = new CrptApi.Document();
        doc.doc_id = "123";
        doc.doc_status = "NEW";
        doc.doc_type = "LP_INTRODUCE_GOODS";
        doc.importRequest = true;
        doc.owner_inn = "1234567890";
        doc.participant_inn = "0987654321";
        doc.producer_inn = "1122334455";
        doc.production_date = "2023-01-01";
        doc.production_type = "TYPE";
        doc.reg_date = "2023-01-01";
        doc.reg_number = "12345";
        doc.description = new CrptApi.Document.Description();
        doc.description.participantInn = "0987654321";
        CrptApi.Document.Product product = new CrptApi.Document.Product();
        product.certificate_document = "CERT_DOC";
        product.certificate_document_date = "2023-01-01";
        product.certificate_document_number = "CERT_NUM";
        product.owner_inn = "1234567890";
        product.producer_inn = "1122334455";
        product.production_date = "2023-01-01";
        product.tnved_code = "TNVED";
        product.uit_code = "UIT";
        product.uitu_code = "UITU";
        doc.products = new CrptApi.Document.Product[]{product};

        return doc;
    }
}
