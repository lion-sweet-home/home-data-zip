package org.example.homedatazip.global.storage.s3;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.listing.dto.S3UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3KeyGenerator keyGenerator;

    /**
     * base-url은 네 yml에 app.s3.base-url 로 들어가있음
     * (예: https://s3.ap-northeast-2.amazonaws.com)
     */
    @Value("${app.s3.base-url:https://s3.ap-northeast-2.amazonaws.com}")
    private String baseUrl;

    /**
     * - 매물 등록 전에 이미지부터 올릴 수 있게 하기 위함
     */
    public S3UploadResponse uploadTemp(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        String bucket = s3Properties.getBucket();
        String key = keyGenerator.tempKey(file.getOriginalFilename());

        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    // content-type 없으면 브라우저에서 다운로드로 잡히는 경우 있어서 넣는 게 좋음
                    .contentType(Objects.toString(file.getContentType(), "application/octet-stream"))
                    .build();

            s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("S3 upload failed", e);
        }

        return new S3UploadResponse(key, publicUrl(bucket, key));
    }


    public String moveToListing(Long listingId, String tempKey, String originalFilenameForExt) {
        String bucket = s3Properties.getBucket();
        String newKey = keyGenerator.listingKey(listingId, originalFilenameForExt);

        // copy
        CopyObjectRequest copyReq = CopyObjectRequest.builder()
                .destinationBucket(bucket)
                .destinationKey(newKey)
                .copySource(bucket + "/" + tempKey)
                .build();
        s3Client.copyObject(copyReq);

        // delete temp
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(tempKey)
                .build();
        s3Client.deleteObject(delReq);

        return newKey;
    }


    public void delete(String key) {
        String bucket = s3Properties.getBucket();
        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(delReq);
    }

    public String bucket() {
        return s3Properties.getBucket();
    }

    public String publicUrl(String bucket, String key) {
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    public String publicUrl(String key) {
        return publicUrl(s3Properties.getBucket(), key);
    }

}
