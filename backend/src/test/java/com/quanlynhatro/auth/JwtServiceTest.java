package com.quanlynhatro.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.quanlynhatro.auth.security.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class JwtServiceTest {
    private static final String SECRET =
            Base64.getEncoder()
                    .encodeToString(
                            "unit-test-secret-with-at-least-32-bytes-123456"
                                    .getBytes(StandardCharsets.UTF_8));

    @Test
    void validJwtIsAccepted() {
        JwtService jwt = new JwtService(SECRET, 60);
        var user = User.withUsername("tenant").password("hash").roles("TENANT").build();
        String token = jwt.generateToken(user);
        assertTrue(jwt.isValid(token, user));
        assertEquals("tenant", jwt.extractUsername(token));
    }

    @Test
    void invalidJwtIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        var user = User.withUsername("tenant").password("hash").roles("TENANT").build();
        assertThrows(RuntimeException.class, () -> jwt.isValid("not-a-jwt", user));
    }

    @Test
    void expiredJwtIsRejected() throws InterruptedException {
        JwtService jwt = new JwtService(SECRET, 0);
        var user = User.withUsername("tenant").password("hash").roles("TENANT").build();
        String token = jwt.generateToken(user);
        Thread.sleep(10);
        assertThrows(RuntimeException.class, () -> jwt.isValid(token, user));
    }
}
