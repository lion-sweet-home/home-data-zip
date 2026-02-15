package org.example.homedatazip.listing.controller;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.storage.s3.S3UploadService;
import org.example.homedatazip.listing.dto.S3UploadResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/listings/images")
public class ListingImageController {

    private final S3UploadService s3UploadService;

    @PostMapping(value = "/temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<S3UploadResponse> uploadTemp(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(s3UploadService.uploadTemp(file));
    }
}