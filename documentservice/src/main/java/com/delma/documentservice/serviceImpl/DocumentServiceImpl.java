package com.delma.documentservice.serviceImpl;

import com.delma.documentservice.entity.Document;
import com.delma.documentservice.repository.DocumentRepository;
import com.delma.documentservice.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final Path storageLocation = Paths.get("uploaded-files");

    private final S3Client s3Client;
    @Value("${aws.s3.bucket.name}") private String bucketName;
    private final S3Presigner s3Presigner;

    @Override
    public List<Document> getDocumentsByUser(String userId) {
        return documentRepository.findByUserId(userId);
    }

    @Override
    public void deleteDocumentFromS3(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(doc.getFilePath())
                .build();

        s3Client.deleteObject(deleteObjectRequest);

        documentRepository.delete(doc);

    }

    @Override
    public Document uploadDocument(MultipartFile file, String userId) throws IOException {
        log.info("Uploading document for user: {}", userId);
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 2. Upload to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();
        log.info("Uploading file to S3 bucket: {}", bucketName);
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        String fileUrl = String.format("https://%s.s3.amazonaws.com/%s",bucketName,fileName);

        Document doc = Document.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .userId(userId)
                .url(fileUrl)
                .filePath(fileName)
                .build();

        return documentRepository.save(doc);

    }

    public String getPresignedUrl(String fileName){


        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName) // This is the 'filePath' stored in your DB
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getObjectRequest)
                .build();

//        return presigner.presignGetObject(presignRequest).url().toString();

        return s3Presigner.presignGetObject(presignRequest).url().toString();

    }
}
