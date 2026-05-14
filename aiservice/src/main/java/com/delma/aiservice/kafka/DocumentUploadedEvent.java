package com.delma.aiservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadedEvent {
    private Long documentId;
    private String userId;
    private String s3Key;        // S3 file path — use to download PDF
    private String fileName;
    private String contentType;  // only process "application/pdf"
}