package com.sciinov.dbms.config;

import com.sciinov.dbms.security.AuthEntryPointJwt;
import com.sciinov.dbms.security.AuthTokenFilter;
import com.sciinov.dbms.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10); // 10 = good balance of security and speed (~100ms/hash)
    }

    /**
     * Custom AccessDeniedHandler that writes JSON directly (same pattern as AuthEntryPointJwt).
     * Using sendError() would strip CORS headers from 403 responses — causing the browser
     * to misreport them as CORS errors instead of authorization failures.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = new ObjectMapper();
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", 403);
            body.put("error", "Forbidden");
            body.put("message", accessDeniedException.getMessage());
            body.put("path", request.getServletPath());
            mapper.writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow ALL OPTIONS preflight requests without authentication (required for CORS)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/auth/signin").permitAll()
                        .requestMatchers("/api/auth/refresh-token").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/validate-reset-token").permitAll()
                        .requestMatchers("/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/forgot-username").permitAll()
                        // Swagger/OpenAPI documentation
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.disable()) // Modern browsers have built-in XSS protection
                );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Trim and split origins properly
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        // Use setAllowedOriginPatterns to support credentials + wildcard if needed
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        // Allow all headers (wildcard) to avoid any header being blocked
        configuration.setAllowedHeaders(List.of("*"));
        // Expose headers that frontend might need to read
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Correlation-ID",
                "X-Total-Count",
                "X-Page-Number",
                "Content-Disposition"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(86400L);  // 24-hour preflight cache — reduces OPTIONS round-trips in production

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Register a CorsFilter at the HIGHEST precedence so CORS headers are
     * present on EVERY response — including 401/403/500 error responses
     * that would otherwise bypass the security-chain CORS config.
     *
     * This fixes the intermittent CORS error seen during file uploads
     * when the token expires or the server returns an error.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> bean =
                new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}