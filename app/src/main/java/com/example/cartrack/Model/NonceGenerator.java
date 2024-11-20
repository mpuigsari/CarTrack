package com.example.cartrack.Model;

import java.security.SecureRandom;
import java.util.Base64;

public class NonceGenerator {
    public static String generateNonce() {
        byte[] randomBytes = new byte[16]; // 128 bits
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
