package com.academic.planning.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希：PBKDF2WithHmacSHA256 + 12 万次迭代 + 16 字节随机盐。
 * 存储格式 pbkdf2_sha256$iterations$salt$hash，比对使用常量时间 isEqual 防时序攻击。
 */
@Component
public class PasswordHasher {

    private static final String PREFIX = "pbkdf2_sha256";
    // 12 万次迭代：足够慢以抵抗暴力破解，又在可接受的登录耗时内
    private static final int DEFAULT_ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String hash(String password) {
        // 每个密码用全新随机盐，相同密码也会得到不同哈希
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, DEFAULT_ITERATIONS);
        return String.join("$", PREFIX, String.valueOf(DEFAULT_ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash));
    }

    public boolean matches(String password, String encoded) {
        if (password == null || encoded == null) {
            return false;
        }
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            // 常量时间比较，避免通过「比对耗时」泄露逐字节是否正确
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("密码哈希算法不可用", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
