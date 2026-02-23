package org.example.homedatazip.global.jwt.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.example.homedatazip.global.jwt.property.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;



@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({ JwtProperties.class })
public class JwtTokenizer {
    private final JwtProperties jwtProperties;

    public String createRefreshToken(String email, List<String> roles, Long userId, String nickname) {
        return createToken(jwtProperties.getRefreshTokenExpiration(),
                email,
                roles,
                userId,
                nickname,
                jwtProperties.getRefreshSecretKey());
    }

    public String createAccessToken(String email, List<String> roles, Long userId, String nickname) {
        return createToken(jwtProperties.getAccessTokenExpiration(),
                email,
                roles,
                userId,
                nickname,
                jwtProperties.getAccessSecretKey());
    }

    public Claims parseAccessToken(String token) {
        return parseToken(token, jwtProperties.getAccessSecretKey());
    }

    public Claims parseRefreshToken(String token) {
        return parseToken(token, jwtProperties.getRefreshSecretKey());
    }

    public String getEmailFromRefreshToken(String token) {
        return parseRefreshToken(token).get("email", String.class);
    }
    public String getEmailFromAccessToken(String token) {
        return parseAccessToken(token).get("email", String.class);
    }

    public Long getUserIdFromAccessToken(String token) {
        Object userId = parseAccessToken(token).get("userId");
        if (userId == null) return null;
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long) return (Long) userId;
        return null;
    }

    public String getNicknameFromAccessToken(String token) {
        return parseAccessToken(token).get("nickname", String.class);
    }

    public boolean validateAccessToken(String token) {
        try{
            parseAccessToken(token);
            return true;
        } catch(Exception e){
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try{
            parseRefreshToken(token);
            return true;
        } catch(Exception e){
            return false;
        }
    }


    private String createToken(Long expiration, String email, List<String> roles, Long userId, String nickname, byte[] secretKey) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expirationDate);
        if (userId != null) {
            builder.claim("userId", userId);
        }
        if (nickname != null && !nickname.isBlank()) {
            builder.claim("nickname", nickname);
        }
        return builder.signWith(getKey(secretKey)).compact();
    }
    private SecretKey getKey(byte[] secretKey){return Keys.hmacShaKeyFor(secretKey);}

    private Claims parseToken(String token, byte[] secretKey){
        return Jwts.parser()
                .verifyWith(getKey(secretKey))
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

}