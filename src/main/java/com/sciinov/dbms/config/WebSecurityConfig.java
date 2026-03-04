package com.sciinov.dbms.config;

import com.sciinov.dbms.security.AuthEntryPointJwt;
import com.sciinov.dbms.security.AuthTokenFilter;
import com.sciinov.dbms.security.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Value("${app.cors.allowed-origins:https://sciinovdbms.com}")
    private String allowedOrigins;

    private List<String> parsedOrigins;

    @javax.annotation.PostConstruct
    public void initializeCorsOrigins() {
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            logger.warn("⚠️  APP_CORS_ALLOWED_ORIGINS is empty, using defaults");
            parsedOrigins = Arrays.asList("http://localhost:5173", "https://sciinovdbms.com");
        } else {
            parsedOrigins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();
        }
        logger.info("✅ CORS Configuration Initialized:");
        logger.info("   Allowed Origins: {}", parsedOrigins);
        logger.info("   Allowed Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
        logger.info("   Allow Credentials: true");
        logger.info("   Preflight Cache: 24 hours (86400 seconds)");
    }

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
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {

        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(10);
    }

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

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Allow ASYNC and ERROR dispatches without re-authorization
                        // These are internal Tomcat dispatches after async processing completes.
                        // The original request was already authenticated on the first pass.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()

                        // Allow CORS preflight requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public APIs
                        .requestMatchers("/api/auth/signin").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/refresh-token").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/auth/validate-reset-token").permitAll()
                        .requestMatchers("/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/forgot-username").permitAll()

                        // Swagger APIs
                        .requestMatchers(
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        // All other APIs require authentication
                        .anyRequest().authenticated()
                )

                .headers(headers -> headers

                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src 'self'"))

                        .referrerPolicy(referrer ->
                                referrer.policy(
                                        ReferrerPolicyHeaderWriter.ReferrerPolicy
                                                .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                        .frameOptions(frame -> frame.deny())

                        .xssProtection(xss -> xss.disable())
                );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        // Use pre-parsed origins from PostConstruct initialization
        if (parsedOrigins == null || parsedOrigins.isEmpty()) {
            logger.error("❌ CORS Origins not initialized! Using fallback defaults");
            parsedOrigins = Arrays.asList("http://localhost:5173", "https://sciinovdbms.com");
        }

        logger.info("🔒 CORS Configuration Applied:");
        logger.info("   ✅ Allowed Origins: {}", parsedOrigins);
        logger.info("   ✅ Allowed Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
        logger.info("   ✅ Allowed Headers: * (All headers)");
        logger.info("   ✅ Allow Credentials: true");
        logger.info("   ✅ Preflight Cache: 24 hours");

        // Set origins using direct list
        configuration.setAllowedOrigins(parsedOrigins);

        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS",
                "HEAD"
        ));

        // Allow all headers including custom ones
        configuration.setAllowedHeaders(List.of("*"));

        // Expose headers that frontend might need to read
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "X-Total-Count",
                "X-Page-Number",
                "X-Correlation-ID",
                "Content-Type",
                "X-Requested-With"
        ));

        configuration.setAllowCredentials(true);

        // Preflight cache for 24 hours (86400 seconds)
        configuration.setMaxAge(86400L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // Register CORS for all paths
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {

        FilterRegistrationBean<CorsFilter> bean =
                new FilterRegistrationBean<>(
                        new CorsFilter(corsConfigurationSource())
                );

        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return bean;
    }
}