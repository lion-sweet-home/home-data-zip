package org.example.homedatazip.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.auth.service.AuthService;
import org.example.homedatazip.global.exception.BusinessException;
import org.example.homedatazip.global.exception.domain.UserErrorCode;
import org.example.homedatazip.role.Role;
import org.example.homedatazip.role.RoleType;
import org.example.homedatazip.role.repository.RoleRepository;
import org.example.homedatazip.user.entity.LoginType;
import org.example.homedatazip.user.entity.User;
import org.example.homedatazip.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;

    @Value("${app.oauth.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        // OAuth2 인증 결과에서 registrationId(google)와 사용자 속성을 꺼냄
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
//        log.info("token 값: {}", token);
        String registrationId = token.getAuthorizedClientRegistrationId(); // google/kakao/naver 구분 가능
        OAuth2User oauth2User = token.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();
//        log.info("속성값들: {}", attributes);

        // registrationId를 프로젝트에서 사용하는 LoginType으로 변환
        LoginType loginType = toLoginType(registrationId);
        if (loginType == null) {
            log.warn("지원하지 않는 OAuth2 registrationId: {}", registrationId);
            response.sendRedirect(frontendRedirectUri + "?error=unsupported_provider");
            return;
        }

        // Google 고유 ID, 이메일, 닉네임을 추출
        String providerId = getProviderId(loginType, attributes);
        String email = getEmail(attributes);
        String nickname = getNickname(attributes, providerId);

        if (providerId == null || email == null || nickname == null) {
            log.warn("OAuth2 attributes 부족: loginType={}, attributes={}", loginType, attributes.keySet());
            response.sendRedirect(frontendRedirectUri + "?error=missing_attributes");
            return;
        }

        // (loginType, providerId)로 조회 → 없으면 같은 이메일 기존 회원 조회 → 없을 때만 신규 소셜 회원 생성
        User user = userRepository.findByLoginTypeAndProviderIdWithRoles(loginType, providerId)
                .or(() -> userRepository.findByEmailWithRoles(email))
                .orElseGet(() -> createOAuthUser(loginType, providerId, email, nickname));

        // AccessToken + Refresh 쿠키 설정 후, AccessToken을 쿼리로 담아 프론트로 리다이렉트 (reissue 불필요)
        String accessToken = authService.issueTokenForOAuthUser(user, response);
        String redirectUrl = frontendRedirectUri + "?token=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private LoginType toLoginType(String registrationId) {
        return switch (registrationId) {
            case "google" -> LoginType.GOOGLE;
            default -> null;
        };
    }

    /** Google: sub */
    private String getProviderId(LoginType loginType, Map<String, Object> attributes) {
        if (loginType == LoginType.GOOGLE) {
            Object sub = attributes.get("sub");
            return sub != null ? sub.toString() : null;
        }
        return null;
    }

    private String getEmail(Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email != null) return email.toString();
        return null;
    }

    // nickname 중복 제거
    private String getNickname(Map<String, Object> attributes, String providerId) {
        Object name = attributes.get("name");
        String base = (name != null && !name.toString().isBlank()) ? name.toString() : "user";
        String nickname = base;
        int suffix = 0;
        while (userRepository.existsByNickname(nickname)) {
            nickname = base + "_" + (providerId != null && providerId.length() >= 6 ? providerId.substring(0, 6) : suffix++);
        }
        return nickname;
    }

    private User createOAuthUser(LoginType loginType, String providerId, String email, String nickname) {
        Role userRole = roleRepository.findByRoleType(RoleType.USER)
                .orElseThrow(() -> new BusinessException(UserErrorCode.ROLE_NOT_FOUND));
        User user = User.createOAuth(loginType, providerId, email, nickname, userRole);
        return userRepository.save(user);
    }
}
