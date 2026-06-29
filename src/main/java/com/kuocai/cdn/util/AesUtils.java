package com.kuocai.cdn.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;

import java.security.SecureRandom;

public class AesUtils {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile String legacyPrivateKey;

    public static void configureLegacyKey(String privateKey) {
        if (privateKey != null && !privateKey.trim().isEmpty()) {
            legacyPrivateKey = privateKey.trim();
        }
    }

    public static AES getAes() {
        String pwdSalt = AesUtils.generatePwdSaltString(16);
        return new AES(Mode.CTS, Padding.PKCS5Padding, resolveLegacyPrivateKey().getBytes(), pwdSalt.getBytes());
    }

    public static AES getAes(String pwdSalt) {
        return new AES(Mode.CTS, Padding.PKCS5Padding, resolveLegacyPrivateKey().getBytes(), pwdSalt.getBytes());
    }

    public static String encryptHex(AES aes, String content) {
        return aes.encryptHex(content);
    }

    public static String decryptStr(AES aes, String ciphertext) {
        return aes.decryptStr(ciphertext, CharsetUtil.CHARSET_UTF_8);
    }

    public static String getPwdSalt(AES aes) {
        return new String(aes.getCipher().getIV());
    }

    public static String generatePwdSaltString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private static String resolveLegacyPrivateKey() {
        String key = firstNotBlank(legacyPrivateKey, System.getProperty("security.password.legacy-aes-key"), System.getenv("PASSWORD_LEGACY_AES_KEY"));
        if (key == null) {
            throw new IllegalStateException("Legacy AES password key is not configured");
        }
        int length = key.getBytes().length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("Legacy AES password key must be 16, 24, or 32 bytes");
        }
        return key;
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
