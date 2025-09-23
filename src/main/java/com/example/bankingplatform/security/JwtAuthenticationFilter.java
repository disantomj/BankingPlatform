package com.example.bankingplatform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        logger.debug("Processing request: {} with auth header: {}", requestURI, authHeader != null ? "present" : "missing");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("No valid Authorization header found for {}", requestURI);
            // No token found, continue filter chain (will be handled by Spring Security)
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the actual token (remove "Bearer " prefix)
        String token = authHeader.substring(7);
        logger.debug("Extracted JWT token for {}: {}", requestURI, token.substring(0, Math.min(20, token.length())) + "...");

        try {
            // Validate token using our JwtProvider
            Jws<Claims> claimsJws = jwtProvider.validateToken(token);
            Claims claims = claimsJws.getBody();

            //Extract user information from token
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            logger.debug("Token validation successful for user: {} with role: {} accessing {}", username, role, requestURI);

            // Create Spring Security authentication object
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            //Set authentication in SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Authentication set successfully for user: {}", username);

        } catch (Exception e) {
            logger.warn("JWT token validation failed for {}: {}", requestURI, e.getMessage());
            // Token is invalid - clear any existing authentication
            SecurityContextHolder.clearContext();
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}