package com.agrogestao.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final Key signingKey;
    private final long expirationMinutes;

    public JwtService(
            @Value("${agrogestao.jwt.secret}") String secret,
            @Value("${agrogestao.jwt.access-token-expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    public record ParsedToken(Long userId, Instant issuedAt) {
    }

    /** Valida assinatura e expiração; lança exceção se o token for inválido. */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith((SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new ParsedToken(Long.parseLong(claims.getSubject()), claims.getIssuedAt().toInstant());
    }

    public Long extractUserId(String token) {
        return parse(token).userId();
    }
}
