package com.ecommerce.ecom_proj.security;

import com.ecommerce.ecom_proj.config.AppProperties;
import com.ecommerce.ecom_proj.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret.startsWith("dev-only-secret")) {
            log.warn("Using the default development JWT secret. Set JWT_SECRET before deploying.");
        }
        // Throws if the secret is shorter than 256 bits, so a weak key fails fast at startup.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(props.jwt().expirationMinutes());
    }

    public String generateToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration.toMillis()))
                .signWith(key)
                .compact();
    }

    /** Verifies the signature and expiry, throwing a JwtException if the token is not valid. */
    public Claims parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
