package org.example.homedatazip.global.storage.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
public class S3Properties {
    /**
     * application.yml 의 spring.cloud.aws.s3.bucket 값 바인딩
     * 예) homedatazip-images
     */
    private String bucket;
}
