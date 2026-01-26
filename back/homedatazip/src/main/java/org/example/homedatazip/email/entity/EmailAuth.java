package org.example.homedatazip.email.entity;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "emailAuth", timeToLive = 300) // 300초(5분) 후 자동 삭제
public class EmailAuth {

    @Id
    private String email; // 이메일을 Key로 사용

    private String authCode;

    private boolean verified;

    public EmailAuth(String email, String authCode, boolean verified) {
        this.email = email;
        this.authCode = authCode;
        this.verified = verified;
    }

    public static EmailAuth create(String email, String authCode) {
        return new EmailAuth(email, authCode, false);
    }

    public void updateVerified(boolean verified) {
        this.verified = verified;
    }
}
