package org.example.homedatazip.global.jwt.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieProvider {

    public void addRefreshCookie(HttpServletResponse response,
                                 String refreshToken,
                                 long maxAgeSeconds,
                                 boolean secure){
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")    //운영할 때 프론트와 백의 도메인이 다르면 None
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    public void clearRefreshCookie(HttpServletResponse response, boolean secure){
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
