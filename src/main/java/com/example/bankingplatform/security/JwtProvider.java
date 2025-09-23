package com.example.bankingplatform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.security.Key;
import java.nio.charset.StandardCharsets;

@Component
public class JwtProvider {

    private final Key key;
    private final long EXPIRATION_TIME;
    private final long REFRESH_THRESHOLD = 300000; // 5 minutes - when to suggest refresh

    public JwtProvider(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration:1800000}") long expiration) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT secret must be provided via environment variable JWT_SECRET");
        }
        if (secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters long for security");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.EXPIRATION_TIME = expiration; // Default 30 minutes for banking security
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> validateToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = validateToken(token).getBody();
            Date expiration = claims.getExpiration();
            long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            return timeUntilExpiration <= REFRESH_THRESHOLD;
        } catch (Exception e) {
            return true; // If we can't parse it, consider it expiring
        }
    }

    public String refreshToken(String username, String role) {
        // Same as generateToken but could add different claims for refresh
        return generateToken(username, role);
    }
}
