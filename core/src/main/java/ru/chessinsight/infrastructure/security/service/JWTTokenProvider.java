package ru.chessinsight.infrastructure.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
final public class JWTTokenProvider {
    private final SecretKey jwtSecretKey;

    public JWTTokenProvider(
            @Value("${jwt.secret}") String jwtSecret
    ) {
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(UUID userId) {
        long jwtAccessExpirationMs = 15 * 60 * 1000;
        return buildToken(userId, jwtAccessExpirationMs);
    }

    public String generateRefreshToken(UUID userId) {
        long jwtRefreshExpirationMs = 7 * 24 * 60 * 1000;
        return buildToken(userId, jwtRefreshExpirationMs);
    }

    private String buildToken(UUID userId, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJwt(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(jwtSecretKey)
                .build()
                .parseClaimsJwt(token)
                .getBody();

        return UUID.fromString(claims.getSubject());
    }
}
