package org.example.homedatazip.listing.dto;

public record S3UploadResponse(
        String key,   // S3 object key (temp/...)
        String url    // public URL (버킷 정책이 GetObject 허용일 때)
) {}
