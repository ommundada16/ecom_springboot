package com.ecommerce.ecom_proj.security;

import com.ecommerce.ecom_proj.config.AppProperties;
import com.ecommerce.ecom_proj.model.Role;
import com.ecommerce.ecom_proj.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-unit-test";

    private static JwtService service(String secret, long expirationMinutes) {
        return new JwtService(new AppProperties(new AppProperties.Jwt(secret, expirationMinutes), null, null));
    }

    private static User user() {
        return User.builder().id(1L).email("ann@shop.com").role(Role.ADMIN).build();
    }

    @Test
    void generatedToken_carriesSubjectAndRole() {
        JwtService jwt = service(SECRET, 60);

        Claims claims = jwt.parseToken(jwt.generateToken(user()));

        assertThat(claims.getSubject()).isEqualTo("ann@shop.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void tamperedToken_isRejected() {
        JwtService jwt = service(SECRET, 60);
        String token = jwt.generateToken(user());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> jwt.parseToken(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String foreign = service("another-secret-another-secret-another-secret", 60).generateToken(user());

        assertThrows(JwtException.class, () -> service(SECRET, 60).parseToken(foreign));
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwt = service(SECRET, -1);

        assertThrows(ExpiredJwtException.class, () -> jwt.parseToken(jwt.generateToken(user())));
    }

    @Test
    void secretShorterThan256Bits_failsFast() {
        assertThrows(WeakKeyException.class, () -> service("too-short", 60));
    }
}
