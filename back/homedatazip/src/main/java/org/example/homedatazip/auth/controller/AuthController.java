package org.example.homedatazip.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homedatazip.auth.dto.LoginRequest;
import org.example.homedatazip.auth.dto.LoginResponse;
import org.example.homedatazip.auth.dto.OAuthApiRequest;
import org.example.homedatazip.auth.service.AuthService;
import org.example.homedatazip.auth.service.GoogleOAuthApiService;
import org.example.homedatazip.global.config.CustomUserDetails;
import org.example.homedatazip.global.jwt.util.CookieProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final GoogleOAuthApiService googleOAuthApiService;
    private final CookieProvider cookieProvider;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest,
                                               HttpServletResponse response) {
        LoginResponse dto = authService.login(loginRequest,response);

        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    /** OAuth API: 프론트가 Google 인가 코드를 보내면 JWT(AT/RT) 발급. */
    @PostMapping("/oauth")
    public ResponseEntity<LoginResponse> oauth(@Valid @RequestBody OAuthApiRequest request,
                                               HttpServletResponse response) {
        LoginResponse dto = googleOAuthApiService.loginWithCode(
                request.code(), request.redirectUri(), response);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> reissue(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                                 HttpServletResponse response,
                                                 CustomUserDetails customUserDetails) {

        LoginResponse dto = authService.reissue(refreshToken,response);
        return  ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response,
                                       @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        if (customUserDetails != null) {
            log.info("CustomUser Id :::" + customUserDetails.getUserId());
            authService.logout(customUserDetails.getUserId(), response);
        } else {
            cookieProvider.clearRefreshCookie(response, false);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
