package org.example.homedatazip.global.storage.s3.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.storage.s3.S3UploadService;
import org.example.homedatazip.listing.dto.S3UploadResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3TestController {

    private final S3UploadService s3UploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<S3UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(s3UploadService.uploadTemp(file));
    }
}
