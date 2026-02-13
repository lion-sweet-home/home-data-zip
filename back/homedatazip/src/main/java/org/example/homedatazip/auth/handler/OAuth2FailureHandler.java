package org.example.homedatazip.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth.frontend-failure-redirect-uri}")
    private String frontendFailureRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        // OAuth2 로그인 실패 시 실패 사유를 쿼리 파라미터로 전달하며 프론트로 리다이렉트
        log.warn("OAuth2 로그인 실패: {}", exception.getMessage());
        if (response.isCommitted()) {
            return;
        }
        String error = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        String redirectUrl = frontendFailureRedirectUri
                + (frontendFailureRedirectUri.contains("?") ? "&" : "?")
                + "error=" + error;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
