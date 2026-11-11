package com.polo42.project_flow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    static final int MIN_SECRET_LENGTH = 64;
    /** SHA-256 do segredo que constava no application.properties dos primeiros commits (comprometido). */
    static final String REVOKED_SECRET_SHA256 = "b73e15c5cf38ed3f01cd5443adc2079e709237d23ffc459ff2b55a749d3570d8";
    static final String PASSWORD_VERSION_CLAIM = "pv";

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpiration;

    @PostConstruct
    void validateSecret() {
        rejectKnownPlaceholders();
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET nao configurado: defina a variavel de ambiente JWT_SECRET com no minimo "
                + MIN_SECRET_LENGTH + " caracteres (veja .env.example).");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT_SECRET muito curto: sao necessarios no minimo " + MIN_SECRET_LENGTH
                + " caracteres para o algoritmo HS512.");
        }
    }

    /** Valores de exemplo e o segredo que ja foi publicado no historico do repositorio nunca podem ser usados. */
    private void rejectKnownPlaceholders() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            return;
        }
        String lower = jwtSecret.toLowerCase(Locale.ROOT);
        boolean placeholder = lower.contains("troque") || lower.contains("changeme")
                || lower.contains("change-me") || lower.contains("exemplo") || lower.contains("example");
        if (placeholder || REVOKED_SECRET_SHA256.equals(sha256(jwtSecret))) {
            throw new IllegalStateException(
                "JWT_SECRET e um valor de exemplo ou ja publicado e nao pode ser usado: gere um segredo proprio "
                + "(ex.: openssl rand -base64 64) e defina em JWT_SECRET.");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(userDetails.getId().toString())
                .claim(PASSWORD_VERSION_CLAIM, userDetails.getPasswordVersion())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return UUID.fromString(claims.getSubject());
    }

    /** Versao da senha vigente quando o token foi emitido (0 se ausente). */
    public long getPasswordVersionFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object value = claims.get(PASSWORD_VERSION_CLAIM);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            // Log
        } catch (ExpiredJwtException ex) {
            // Log
        } catch (UnsupportedJwtException ex) {
            // Log
        } catch (IllegalArgumentException ex) {
            // Log
        }
        return false;
    }
}