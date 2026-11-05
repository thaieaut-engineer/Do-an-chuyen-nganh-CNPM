package bus.auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FORMAT_ID = "pbkdf2-sha256";
    private static final int ITERATIONS = 240_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int MIN_ITERATIONS = 100_000;
    private static final int MAX_ITERATIONS = 1_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derivedKey = deriveKey(password, salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return FORMAT_ID + "$" + ITERATIONS + "$"
                + encoder.encodeToString(salt) + "$"
                + encoder.encodeToString(derivedKey);
    }

    public static boolean verify(char[] password, String encodedHash) {
        if (password == null || password.length == 0
                || encodedHash == null || encodedHash.isBlank()) {
            return false;
        }

        String[] parts = encodedHash.split("\\$", -1);
        if (parts.length != 4 || !FORMAT_ID.equals(parts[0])) {
            throw new IllegalArgumentException("Định dạng hash mật khẩu không được hỗ trợ.");
        }

        final int iterations;
        final byte[] salt;
        final byte[] expectedKey;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getUrlDecoder().decode(parts[2]);
            expectedKey = Base64.getUrlDecoder().decode(parts[3]);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Hash mật khẩu lưu trong database không hợp lệ.", exception);
        }
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS
                || salt.length < SALT_BYTES || expectedKey.length != KEY_BITS / 8) {
            throw new IllegalArgumentException("Tham số hash mật khẩu lưu trong database không hợp lệ.");
        }

        byte[] actualKey = deriveKey(password, salt, iterations);
        return MessageDigest.isEqual(expectedKey, actualKey);
    }

    private static byte[] deriveKey(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("JDK không hỗ trợ thuật toán PBKDF2-HMAC-SHA256.", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}
