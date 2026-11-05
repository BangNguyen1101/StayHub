package com.quanlynhatro;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Local utility: prints a BCrypt hash for the password passed as the first argument. The password
 * is never stored in source code.
 */
public final class PasswordHashGenerator {
    private PasswordHashGenerator() {}

    public static void main(String[] args) {
        if (args.length != 1 || args[0].isBlank()) {
            System.err.println("Usage: PasswordHashGenerator <password>");
            return;
        }
        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}
