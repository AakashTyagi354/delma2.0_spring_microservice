package com.delma.documentservice.controller;

import com.delma.common.dto.ApiResponse;
import com.delma.documentservice.response.DocumentResponse;
import com.delma.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")

@RequiredArgsConstructor
public class DocumentController {

        private final DocumentService documentService;

        @GetMapping("/getall-documents/{userId}")
        public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAllDocuments(@PathVariable String userId) {

            List<DocumentResponse> docs = documentService.getDocumentsByUser(userId);
            return ResponseEntity.ok(ApiResponse.success(docs,"Getting all the documents for the user"));
        }

        @DeleteMapping("/delete-document/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
            documentService.deleteDocumentFromS3(id);
            return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
        }

        @PostMapping("/upload")
        public ResponseEntity<ApiResponse<DocumentResponse>> upload(@RequestParam("file")MultipartFile file, @RequestParam String userId) throws IOException {

                DocumentResponse doc = documentService.uploadDocument(file, userId);
                return ResponseEntity.ok(ApiResponse.success(doc,"Provided document upload successfully"));
        }

}
