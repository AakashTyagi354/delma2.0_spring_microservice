package com.delma.documentservice.service;


import com.delma.documentservice.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DocumentService {
     List<DocumentResponse> getDocumentsByUser(String userId);
     void deleteDocumentFromS3(Long id);
    DocumentResponse uploadDocument(MultipartFile file, String userId) throws IOException;

}
