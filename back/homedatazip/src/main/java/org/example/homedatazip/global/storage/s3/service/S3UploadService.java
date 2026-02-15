package org.example.homedatazip.global.storage.s3;

import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.ListingErrorCode;
import org.example.homedatazip.listing.dto.S3UploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URI;
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
                    .contentType(Objects.toString(file.getContentType(), "application/octet-stream"))
                    .build();

            s3Client.putObject(putReq, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("S3 upload failed", e);
        }

        return new S3UploadResponse(key, publicUrl(bucket, key));
    }

    /**
     * tempKey -> listings/{listingId}/... 로 이동
     * - tempKey가 URL로 들어와도 key만 뽑아서 처리
     * - copy 전에 HEAD로 존재 확인
     * - NoSuchKeyException 등을 BusinessException으로 변환
     */
    public String moveToListing(Long listingId, String tempKey, String originalFilenameForExt) {
        String bucket = s3Properties.getBucket();

        // 1) tempKey 정규화 (URL 들어와도 key만 추출)
        String normalizedTempKey = normalizeKey(tempKey);

        // 2) 존재 확인 (없으면 400)
        if (!exists(bucket, normalizedTempKey)) {
            throw new BusinessException(ListingErrorCode.IMAGE_TEMP_NOT_FOUND);
        }

        String newKey = keyGenerator.listingKey(listingId, originalFilenameForExt);

        try {
            // 3) copy
            CopyObjectRequest copyReq = CopyObjectRequest.builder()
                    .destinationBucket(bucket)
                    .destinationKey(newKey)
                    .copySource(bucket + "/" + normalizedTempKey)
                    .build();
            s3Client.copyObject(copyReq);

            // 4) delete temp (실패해도 새 키는 이미 생겼으니, 여기서 터지면 운영이 더 귀찮아짐)
            try {
                DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(normalizedTempKey)
                        .build();
                s3Client.deleteObject(delReq);
            } catch (S3Exception ignored) {
                // temp 삭제 실패는 치명적이지 않아서 무시 (원하면 로그만 찍어)
            }

            return newKey;

        } catch (NoSuchKeyException e) {
            // copy 단계에서 터지는 케이스도 있음
            throw new BusinessException(ListingErrorCode.IMAGE_TEMP_NOT_FOUND);
        } catch (S3Exception e) {
            // 권한/버킷정책/리전/기타 에러
            throw new BusinessException(ListingErrorCode.IMAGE_MOVE_FAILED);
        }
    }

    public void delete(String key) {
        String bucket = s3Properties.getBucket();
        String normalizedKey = normalizeKey(key);

        DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();
        s3Client.deleteObject(delReq);
    }

    public String getBucket() {
        return s3Properties.getBucket();
    }

    public String publicUrl(String bucket, String key) {
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }

    public String publicUrl(String key) {
        return publicUrl(s3Properties.getBucket(), key);
    }

    /**
     * tempKey로 URL이 들어오는 사고 방지:
     * - https://bucket.s3.../temp/xxx.png -> temp/xxx.png
     * - /temp/xxx.png -> temp/xxx.png
     * - temp/xxx.png -> temp/xxx.png
     */
    private String normalizeKey(String maybeUrlOrKey) {
        if (maybeUrlOrKey == null) return null;

        String s = maybeUrlOrKey.trim();

        // 1) URL이면 path만 추출
        if (s.startsWith("http://") || s.startsWith("https://")) {
            try {
                URI uri = URI.create(s);
                String path = uri.getPath(); // "/temp/xxx.png"
                if (path != null && path.startsWith("/")) path = path.substring(1);
                return path;
            } catch (Exception e) {
                // URL 파싱 실패면 그냥 원본으로
                return s;
            }
        }

        // 2) "/temp/.." 형태면 앞 슬래시 제거
        if (s.startsWith("/")) s = s.substring(1);
        return s;
    }

    /**
     * S3에 객체 존재하는지 확인 (HEAD)
     */
    private boolean exists(String bucket, String key) {
        if (key == null || key.isBlank()) return false;

        try {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.headObject(req);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            // 404(NotFound)도 여기로 올 수 있음
            return e.statusCode() != 404 ? false : false;
        }
    }
}
