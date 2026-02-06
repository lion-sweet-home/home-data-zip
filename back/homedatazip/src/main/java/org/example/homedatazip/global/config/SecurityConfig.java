package org.example.homedatazip.global.config;

import org.example.homedatazip.auth.handler.OAuth2SuccessHandler;
import org.example.homedatazip.global.jwt.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                                           CorsProperties corsProperties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(corsProperties)))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2SuccessHandler))
                .authorizeHttpRequests(auth -> auth
                        // OAuth2 콜백 (Google 등)
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        // 로그인 사용자 전용 (favorite)
                        .requestMatchers("/api/users/me/**").authenticated()
                        // 회원가입·로그인 등
                        .requestMatchers("/api/users/**").permitAll()
                        // 로그인
                        .requestMatchers("/api/auth/**").permitAll()

                        .requestMatchers("/api/bus-stations/**").permitAll()

                        .requestMatchers("/api/apartments/**").permitAll()

                        .requestMatchers("/api/hospitals/**").permitAll()

                        // 지하철역 부분 검색
                        .requestMatchers("/api/subway/stations/**").permitAll()

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/test/**").permitAll()

                        //전월세 조회
                        .requestMatchers("/api/rent/**").permitAll()

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
