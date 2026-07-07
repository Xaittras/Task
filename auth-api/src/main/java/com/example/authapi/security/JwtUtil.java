package com.example.authapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Small helper around jjwt for issuing and validating the tokens used
 * by Service A. The secret is provided via the JWT_SECRET env var
 * (mapped to app.jwt-secret in application.yml) and must be at least
 * 32 chars for HS256.
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt-secret}") String secret,
            @Value("${app.jwt-expiration-ms:3600000}") long expirationMs
    ) {
        // Pad the secret if it's shorter than required so local/dev secrets
        // still work; in real deployments provide a long random JWT_SECRET.
        String normalized = secret;
        while (normalized.getBytes(StandardCharsets.UTF_8).length < 32) {
            normalized = normalized + normalized;
        }
        this.key = Keys.hmacShaKeyFor(normalized.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException | JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
