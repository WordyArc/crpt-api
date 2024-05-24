package com.wordyarc;

import java.io.*;
import java.util.concurrent.*;

public class App {

    public static void main(String[] args) throws InterruptedException, IOException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 5);

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

        String signature = "testSignature";
        api.createDocument(doc, signature);
        api.dispose();
    }

}
