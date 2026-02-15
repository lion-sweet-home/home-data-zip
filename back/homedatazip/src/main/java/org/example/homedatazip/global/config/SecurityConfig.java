package org.example.homedatazip.global.config;

import org.example.homedatazip.auth.handler.OAuth2FailureHandler;
import org.example.homedatazip.auth.handler.OAuth2SuccessHandler;
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
                                           OAuth2SuccessHandler oAuth2SuccessHandler,
                                           OAuth2FailureHandler oAuth2FailureHandler,
                                           CorsProperties corsProperties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                .authorizeHttpRequests(auth -> auth
                        // OAuth2 콜백 (Google 등)
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        // 로그인 사용자 전용 (favorite)
                        .requestMatchers("/api/users/me/**").authenticated()
                        // 회원가입·로그인 등
                        .requestMatchers("/api/users/**").permitAll()
                        // 로그인
                        .requestMatchers("/api/auth/**").permitAll()

                        // 버스정류장
                        .requestMatchers("/api/bus-stations/**").permitAll()

                        .requestMatchers("/api/apartments/**").permitAll()

                        .requestMatchers("/api/hospitals/**").permitAll()

                        // 지하철역 부분 검색
                        .requestMatchers("/api/subway/stations/**").permitAll()

                        // 학교 지역 검색·반경 내 아파트
                        .requestMatchers("/api/schools/**").permitAll()

                        // 매매
                        .requestMatchers(HttpMethod.GET, "/api/listings/me/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/listings/create/**").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()

                        //S3 테스트 진행 후 셀로로 교체
                        .requestMatchers(HttpMethod.POST, "/api/s3/**").permitAll()

                        // 구독, 추후 로그인한 사람에 한해 가능
                        .requestMatchers(
                                "/api/subscriptions/billing/success",
                                "/api/subscriptions/billing/fail"
                        ).hasRole("USER")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/test/**").permitAll()



                        .requestMatchers("/api/regions/**", "/api/apartment/trade-sale/**").permitAll()

                        //전월세 조회
                        .requestMatchers("/api/rent/**").permitAll()

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
