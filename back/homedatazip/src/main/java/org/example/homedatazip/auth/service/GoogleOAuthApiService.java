package org.example.homedatazip.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.auth.dto.LoginResponse;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.AuthErrorCode;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.user.entity.LoginType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthApiService {

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URI = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final WebClient.Builder webClientBuilder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * Google 인가 코드로 토큰 교환 → userinfo 조회 → 회원 생성/조회 → JWT 발급.
     */
    @Transactional
    public LoginResponse loginWithCode(String code, String redirectUri, HttpServletResponse response) {
        String accessToken = exchangeCodeForAccessToken(code, redirectUri);
        Map<String, Object> userInfo = fetchUserInfo(accessToken);

        String providerId = getString(userInfo, "id");
        String email = getString(userInfo, "email");
        String name = getString(userInfo, "name");
        if (providerId == null || email == null) {
            log.warn("Google userinfo 부족: {}", userInfo.keySet());
            throw new BusinessException(AuthErrorCode.OAUTH_INVALID_CODE);
        }
        String nicknameBase = name != null && !name.isBlank() ? name : "user";
        String nickname = ensureUniqueNickname(nicknameBase, providerId);

        User user = userRepository.findByLoginTypeAndProviderIdWithRoles(LoginType.GOOGLE, providerId)
                .or(() -> userRepository.findByEmailWithRoles(email))
                .orElseGet(() -> createOAuthUser(providerId, email, nickname));

        String jwtAccessToken = authService.issueTokenForOAuthUser(user, response);
        return new LoginResponse(jwtAccessToken);
    }

    private String exchangeCodeForAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            Map<String, Object> body = webClientBuilder.build()
                    .post()
                    .uri(GOOGLE_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (body == null) throw new BusinessException(AuthErrorCode.OAUTH_INVALID_CODE);
            Object at = body.get("access_token");
            return at != null ? at.toString() : null;
        } catch (WebClientResponseException e) {
            log.warn("Google token exchange failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(AuthErrorCode.OAUTH_INVALID_CODE);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(String accessToken) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(GOOGLE_USERINFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Google userinfo failed: status={}", e.getStatusCode());
            throw new BusinessException(AuthErrorCode.OAUTH_INVALID_CODE);
        }
    }

    private static String getString(Map<String, Object> map, String key) {
        Object v = map != null ? map.get(key) : null;
        return v != null ? v.toString() : null;
    }

    private String ensureUniqueNickname(String base, String providerId) {
        String nickname = base;
        int suffix = 0;
        while (userRepository.existsByNickname(nickname)) {
            nickname = base + "_" + (providerId != null && providerId.length() >= 6 ? providerId.substring(0, 6) : suffix++);
        }
        return nickname;
    }

    private User createOAuthUser(String providerId, String email, String nickname) {
        Role userRole = roleRepository.findByRoleType(RoleType.USER)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ROLE_NOT_FOUND));
        User user = User.createOAuth(LoginType.GOOGLE, providerId, email, nickname, userRole);
        return userRepository.save(user);
    }
}
