package com.delma.documentservice.kafka;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadedEvent {
    private Long documentId;      // DB id of the document
    private String userId;        // which patient owns this
    private String s3Key;         // S3 file path e.g. "1234567_report.pdf"
    private String fileName;      // original file name
    private String contentType;    // "application/pdf" or "image/png" etc.
}
