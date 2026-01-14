package com.delma.documentservice.controller;

import com.delma.documentservice.entity.Document;
import com.delma.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")

@RequiredArgsConstructor
public class DocumentController {

        private final DocumentService documentService;

        @GetMapping("/getall-documents/{userId}")
        public ResponseEntity<?> getAllDOcuments(@PathVariable String userId) {
            log.info("Fetching documents for userId: {}", userId);
            List<Document> docs = documentService.getDocumentsByUser(userId);

            docs.forEach(doc -> doc.setUrl(documentService.getPresignedUrl(doc.getFilePath())));
            log.info("Retrieved {} documents for userId: {}", docs.size(), userId);


            return ResponseEntity.ok(docs);
        }

        @DeleteMapping("/delete-document/{id}")
        public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
            documentService.deleteDocumentFromS3(id);
            return ResponseEntity.ok("Document deleted successfully");
        }

        @PostMapping("/upload")
        public ResponseEntity<?> upload(@RequestParam("file")MultipartFile file, @RequestParam String userId){
            try {
                Document doc = documentService.uploadDocument(file, userId);
                return ResponseEntity.ok(doc);
            } catch (Exception e) {
                return ResponseEntity.status(500).body("Error uploading document: " + e.getMessage());
            }
        }

}
