package ru.chessinsight.infrastructure.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import ru.chessinsight.infrastructure.security.JWTAuthenticationFilter;
import ru.chessinsight.infrastructure.web.dto.ProblemDetails;
import ru.chessinsight.infrastructure.web.exception.ProblemDetailsFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JWTAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JWTAuthenticationFilter filter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = filter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v1/swagger-ui.html",
                                "/v1/swagger-ui/**",
                                "/v1/openapi.yaml",
                                "/v1/api-docs/**",
                                "/api/actuator/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                            ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST, "/v1/users"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST, "/v1/auth/sessions"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.DELETE, "/v1/auth/sessions"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST, "/v1/auth/tokens"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST, "/v1/auth/recovery/request"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST, "/v1/auth/recovery/confirm"
                        ).permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            ProblemDetails details = ProblemDetailsFactory.create(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Unauthorized",
                    request.getRequestURI()
            );
            response.setStatus(org.springframework.http.HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), details);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            ProblemDetails details = ProblemDetailsFactory.create(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Forbidden",
                    request.getRequestURI()
            );
            response.setStatus(org.springframework.http.HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), details);
        };
    }
}
