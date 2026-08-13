package com.cakedelight.authservice.service;

import com.cakedelight.authservice.entity.Role;
import com.cakedelight.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "this-is-a-test-secret-key-that-is-long-enough-for-hs256";
    private static final long TEST_EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", TEST_EXPIRATION_MS);
    }

    @Test
    void generateToken_producesTokenWithExpectedClaims() {
        User user = new User();
        user.setId(42L);
        user.setEmail("cake.lover@example.com");
        user.setRole(Role.CUSTOMER);

        String token = jwtService.generateToken(user);

        SecretKey verificationKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token);
        Claims claims = parsed.getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("cake.lover@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(TEST_EXPIRATION_MS);
    }
}
