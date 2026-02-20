package com.sciinov.dbms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Additional CORS filter to ensure proper CORS headers are set
 * This is a backup to the Spring Security CORS configuration
 */
@Component
public class CorsFilter extends OncePerRequestFilter {

    @Value("${app.cors.allowed-origins:https://sciinovdbms.com,http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Get the origin from the request
        String origin = request.getHeader("Origin");

        // Parse allowed origins
        List<String> allowedOriginsList = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.isEmpty())
                .toList();

        // Check if the origin is allowed
        if (origin != null && allowedOriginsList.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(origin))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
            response.setHeader("Access-Control-Allow-Headers",
                "Authorization, Content-Type, Accept, X-Requested-With, X-User-Id, X-User-Name, X-Correlation-ID, X-CSRF-Token, Origin, Cache-Control, Access-Control-Request-Method, Access-Control-Request-Headers");
            response.setHeader("Access-Control-Expose-Headers",
                "Authorization, X-Correlation-ID, X-Total-Count, X-Page-Number");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Max-Age", "3600");
        }

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        filterChain.doFilter(request, response);
    }
}

