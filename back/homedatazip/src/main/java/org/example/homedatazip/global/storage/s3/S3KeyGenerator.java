package org.example.homedatazip.global.storage.s3;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class S3KeyGenerator {

    /**
     * 업로드 직후에는 temp/ 아래에 저장
     * 예) temp/2026-02-15/uuid.png
     */
    public String tempKey(String originalFilename) {
        String ext = extensionOrEmpty(originalFilename);
        return "temp/" + LocalDate.now() + "/" + UUID.randomUUID() + ext;
    }

    /**
     * 매물 확정 시 listings/{listingId}/ 아래로 이동(실제로는 copy + delete)
     */
    public String listingKey(Long listingId, String originalFilename) {
        String ext = extensionOrEmpty(originalFilename);
        return "listings/" + listingId + "/" + UUID.randomUUID() + ext;
    }

    private String extensionOrEmpty(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0) return "";
        return filename.substring(idx); // ".png"
    }
}
