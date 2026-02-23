package org.example.homedatazip.global.jwt.property;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
@NoArgsConstructor
@Getter
public class JwtProperties {
    private byte[] accessSecretKey;
    private byte[] refreshSecretKey;
    @Setter
    private long accessTokenExpiration;
    @Setter
    private long refreshTokenExpiration;

    public void setAccessSecretKey(String accessSecretKey) {
        this.accessSecretKey = accessSecretKey.getBytes(StandardCharsets.UTF_8);
    }

    public void setRefreshSecretKey(String refreshSecretKey) {
        this.refreshSecretKey = refreshSecretKey.getBytes(StandardCharsets.UTF_8);
    }
}
