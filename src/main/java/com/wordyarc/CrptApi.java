package com.wordyarc;

import java.io.*;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.*;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.*;
import org.apache.hc.core5.http.io.entity.*;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        if (timeUnit == null && requestLimit <= 0) {
            throw new IllegalArgumentException("timeUnit не должен быть null, и requestLimit должен быть положительным числом");
        }

        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClients.createDefault();

        startRateLimiter();
    }

    private void startRateLimiter() {
        long interval = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (semaphore) {
                semaphore.release(requestLimit - semaphore.availablePermits());
            }
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();

        try {
            HttpPost post = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Signature", signature);

            StringEntity entity = new StringEntity(objectMapper.writeValueAsString(document), ContentType.APPLICATION_JSON);
            post.setEntity(entity);

            HttpClientResponseHandler<String> responseHandler = response -> {
                int status = response.getCode();
                if (status >= 200 && status < 300) {
                    return "Response: " + new String(response.getEntity().getContent().readAllBytes());
                } else {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    return "Error: " + status + " Body: " + responseBody;
                    //throw new IOException("Ошибка: " + status + " Body: " + responseBody);
                }
            };

            String responseBody = httpClient.execute(post, responseHandler);
            System.out.println(responseBody);
        } finally {
            semaphore.release();
        }
    }

    public void dispose() throws IOException {
        httpClient.close();
        scheduler.shutdown();
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type = "LP_INTRODUCE_GOODS";
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}










