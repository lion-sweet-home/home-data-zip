package org.example.homedatazip.global.config;

import org.example.homedatazip.global.jwt.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           CorsProperties corsProperties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 닉네임/이메일 중복 확인, 회원가입,
                        // 이메일 인증 코드 발송/확인, 리프레시
                        // 로그인, 로그아웃, OAuth
                        .requestMatchers("/api/users/check-nickname", "/api/users/check-email", "/api/users/register",
                                "/api/users/email-verification", "/api/users/verify-email-code", "/api/auth/refresh",
                                "/api/auth/login", "/api/auth/logout", "/api/auth/oauth").permitAll()

                        // 아파트/지하철/버스/병원/학교 검색
                        .requestMatchers("/api/apartments/**", "/api/subway/stations/**", "/api/bus-stations/**",
                                "/api/hospitals/**", "/api/schools/**").permitAll()

                        // 관심매물
                        .requestMatchers("/api/users/me/favorite/**").authenticated()

                        // 매물등록
                        // 지하철역 부분 검색
                        .requestMatchers("/api/subway/stations/**").permitAll()

                        // 학교 지역 검색·반경 내 아파트
                        .requestMatchers("/api/schools/**").permitAll()
                        .requestMatchers("/api/v1/recommendations").permitAll()

                        // 매매
                        .requestMatchers(HttpMethod.GET, "/api/listings/me/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/listings/create/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()

                        // 지역, 매매/전월세
                        .requestMatchers("/api/regions/**", "/api/apartment/trade-sale/**", "/api/rent/**").permitAll()

                        //S3 테스트 진행 후 셀러 교체
                        .requestMatchers(HttpMethod.POST, "/api/s3/**").hasRole("SELLER")
                        .requestMatchers("/api/s3/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/payments/billing-key/confirm").permitAll()

                        .requestMatchers(
                                "/api/subscriptions/billing/success",
                                "/api/subscriptions/billing/fail"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/payments/billing-key/confirm").permitAll()

                        // 구독 관련 나머지는 인증 필수
                        .requestMatchers("/api/subscriptions/phone-auth/**").permitAll()
                        .requestMatchers("/api/subscriptions/**").authenticated()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/test/**").permitAll()


                        // 테스트용 클라이언트 페이지
                        // todo: 나중에 삭제 예정
                        .requestMatchers("/test-chat.html").permitAll()
                        // 웹소켓 연결점
                        .requestMatchers("/ws-stomp/**").permitAll()

                        .requestMatchers("/", "/apartment.html", "/css/", "/js/", "/images/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setMaxAge(corsProperties.getMaxAge());

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // 모든 경로에 적용
        return source;
    }
}
