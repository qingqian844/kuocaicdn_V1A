package com.kuocai.cdn.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    public static final String CLAIMS_KEY = "jwtClaims";

    private static volatile String rsaPublicKey;
    private static volatile String rsaPrivateKey;
    private static volatile Algorithm algorithm;
    private static volatile JWTVerifier verifier;

    public static synchronized void configure(String publicKey, String privateKey) {
        if (isBlank(publicKey) || isBlank(privateKey)) {
            return;
        }
        rsaPublicKey = publicKey;
        rsaPrivateKey = privateKey;
        algorithm = null;
        verifier = null;
    }

    public static String getToken(Map<String, String> map) {
        return getToken(map, Calendar.DATE, 1);
    }

    public static String getToken(Map<String, String> map, Integer calenderType, Integer count) {
        Calendar instance = Calendar.getInstance();
        instance.add(calenderType, count);
        JWTCreator.Builder builder = JWT.create().withIssuedAt(new Date());
        map.forEach(builder::withClaim);
        builder.withExpiresAt(instance.getTime());
        return builder.sign(getAlgorithm());
    }

    public static void validate(String token) {
        getVerifier().verify(token);
    }

    public static Map<String, String> getValidationsObjects(String token) {
        DecodedJWT verify = getVerifier().verify(token);
        Map<String, Claim> claims = verify.getClaims();
        Map<String, String> result = new HashMap<>();
        claims.forEach((k, v) -> result.put(k, v.asString()));
        return result;
    }

    private static Algorithm getAlgorithm() {
        Algorithm current = algorithm;
        if (current == null) {
            synchronized (JwtUtil.class) {
                current = algorithm;
                if (current == null) {
                    String publicKeyText = firstNotBlank(rsaPublicKey, System.getProperty("security.jwt.public-key"), System.getenv("JWT_PUBLIC_KEY"));
                    String privateKeyText = firstNotBlank(rsaPrivateKey, System.getProperty("security.jwt.private-key"), System.getenv("JWT_PRIVATE_KEY"));
                    if (isBlank(publicKeyText) || isBlank(privateKeyText)) {
                        throw new IllegalStateException("JWT RSA keys are not configured");
                    }
                    RSAPublicKey publicKey = loadPublicKey(publicKeyText);
                    RSAPrivateKey privateKey = loadPrivateKey(privateKeyText);
                    current = Algorithm.RSA256(publicKey, privateKey);
                    algorithm = current;
                    verifier = JWT.require(current).build();
                }
            }
        }
        return current;
    }

    private static JWTVerifier getVerifier() {
        JWTVerifier current = verifier;
        if (current == null) {
            getAlgorithm();
            current = verifier;
        }
        return current;
    }

    private static RSAPublicKey loadPublicKey(String key) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(base64Decode(key));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    private static RSAPrivateKey loadPrivateKey(String key) {
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(base64Decode(key));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    private static byte[] base64Decode(String key) {
        return Base64.getDecoder().decode(normalizeKey(key));
    }

    private static String normalizeKey(String key) {
        return key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static Map<String, String> convertToMap(Object obj) {
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> tempMap = (Map<?, ?>) obj;
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    result.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
            return result;
        }
        return null;
    }

    public static Map<String, String> claimsFormRequest(HttpServletRequest request) {
        Object claims = request.getAttribute(CLAIMS_KEY);
        if (claims == null) {
            return null;
        }
        return convertToMap(claims);
    }
}
