package com.qrsignature.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String projectId, String userId, String fileId, String metaCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("projectId", projectId);
        claims.put("userId", userId);
        claims.put("fileId", fileId);
        claims.put("metaCode", metaCode);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractProjectId(String token) {
        return extractClaims(token).get("projectId", String.class);
    }

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public String extractFileId(String token) {
        return extractClaims(token).get("fileId", String.class);
    }

    public String extractMetaCode(String token) {
        return extractClaims(token).get("metaCode", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            extractClaims(token);
            if (isTokenExpired(token)) {
                throw new RuntimeException("token已过期");
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}