package com.delma.documentservice.service;

import com.delma.documentservice.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {
    public List<Document> getDocumentsByUser(String userId);
    public void deleteDocumentFromS3(Long id);
    public Document uploadDocument(MultipartFile file, String userId) throws IOException;
    public String getPresignedUrl(String fileName);
}
