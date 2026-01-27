package org.example.homedatazip.Auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {
    private final StringRedisTemplate redis;

    private static final String KEY_PREFIX = "refresh:user:";

    private String key(Long userId){
        return KEY_PREFIX + userId;
    }
    public void save(Long userId, String refreshToken, Duration ttl){
        redis.opsForValue().set(key(userId), refreshToken, ttl);
    }

    public String find(Long userId){
        return redis.opsForValue().get(key(userId));
    }

    public void delete(Long userId){
        redis.delete(key(userId));
    }
}
