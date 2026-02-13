package org.example.homedatazip.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.auth.dto.LoginRequest;
import org.example.homedatazip.auth.dto.LoginResponse;
import org.example.homedatazip.auth.repository.RefreshTokenRedisRepository;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.AuthErrorCode;
import org.example.homedatazip.global.jwt.property.JwtProperties;
import org.example.homedatazip.global.jwt.util.CookieProvider;
import org.example.homedatazip.global.jwt.util.JwtTokenizer;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.UserRole;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenizer jwtTokenizer;
    private final UserRepository userRepository;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final PasswordEncoder passwordEncoder;
    private final CookieProvider cookieProvider;
    private final JwtProperties jwtProperties;


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest,
                               HttpServletResponse response) {
        User user = findUserByEmail(loginRequest.email());

        if(!passwordEncoder.matches(loginRequest.password(), user.getPassword())){
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIAL);
        }

        List<String> roles = stringFromUserRole(user);

        String accessToken = jwtTokenizer.createAccessToken(loginRequest.email(),  roles);
        String refreshToken = jwtTokenizer.createRefreshToken(loginRequest.email(),  roles);


        Duration ttl = getRefreshTokenTtl();
        refreshTokenRedisRepository.save(user.getId(), refreshToken, ttl);

        addCookieAndSetHeader(response, refreshToken, accessToken);

        return new LoginResponse(accessToken);
    }

    @Transactional
    public LoginResponse reissue(String refreshToken,
                                 HttpServletResponse response){

        if(refreshToken == null || refreshToken.isBlank()){
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIAL);
        }

        if(!jwtTokenizer.validateRefreshToken(refreshToken)){
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN_EXCEPTION);
        }

        String email = jwtTokenizer.getEmailFromRefreshToken(refreshToken);
        User user = findUserByEmail(email);

        String redisRefreshToken = refreshTokenRedisRepository.find(user.getId());
        if (redisRefreshToken == null){
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN_EXCEPTION);
        }
        if (!redisRefreshToken.equals(refreshToken)){
            refreshTokenRedisRepository.delete(user.getId());
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN_EXCEPTION);
        }

        List<String> roles = stringFromUserRole(user);

        String newAccess = jwtTokenizer.createAccessToken(email, roles);
        String newRefresh = jwtTokenizer.createRefreshToken(email, roles);

        Duration ttl = getRefreshTokenTtl();
        refreshTokenRedisRepository.save(user.getId(), newRefresh, ttl);

        addCookieAndSetHeader(response, newRefresh, newAccess);

        return new LoginResponse(newAccess);
    }

    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        log.info("logout userId={}", userId);
        refreshTokenRedisRepository.delete(userId);
        cookieProvider.clearRefreshCookie(response, false);
    }

    /** 소셜 로그인 성공 시: User 기준으로 AccessToken + RefreshToken 발급, 쿠키 설정. 반환값은 프론트 리다이렉트 URL에 붙일 AccessToken. */
    @Transactional(readOnly = true)
    public String issueTokenForOAuthUser(User user, HttpServletResponse response) {
        List<String> roles = stringFromUserRole(user);
        String accessToken = jwtTokenizer.createAccessToken(user.getEmail(), roles);
        String refreshToken = jwtTokenizer.createRefreshToken(user.getEmail(), roles);
        Duration ttl = getRefreshTokenTtl();
        refreshTokenRedisRepository.save(user.getId(), refreshToken, ttl);
        addCookieAndSetHeader(response, refreshToken, accessToken);
        return accessToken;
    }

    private User findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }

    private List<String> stringFromUserRole(User user){
        return user.getRoles().stream()
                .map(UserRole::getRole)
                .map(Role::getRoleType)
                .distinct()
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    private void addCookieAndSetHeader(HttpServletResponse response,
                                       String refreshToken,
                                       String accessToken){
        long maxAgeSeconds = jwtProperties.getRefreshTokenExpiration() /1000;

        boolean secure = false;     //운영할 떈 true 바꾸기
        cookieProvider.addRefreshCookie(
                response,
                refreshToken,
                maxAgeSeconds,
                secure);

        if (accessToken != null) {
            response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
    }
    private Duration getRefreshTokenTtl(){
        long expiration = jwtProperties.getRefreshTokenExpiration();
        Duration ttl = Duration.ofMillis(expiration);;
        return ttl;
    }
}
