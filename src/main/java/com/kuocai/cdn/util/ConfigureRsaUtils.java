package com.kuocai.cdn.util;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author xiaobo
 * @date 2023/3/22
 */
public class ConfigureRsaUtils {

    private static final String CHUNKED_PREFIX = "RSA_CHUNKED_V1:";

    private static final String CHUNK_SEPARATOR = ".";

    private static final int ENCRYPT_BLOCK_SIZE = 100;

    private static volatile String configuredPrivateKey;

    private static volatile String configuredPublicKey;

    public static void configure(String privateKey, String publicKey) {
        configuredPrivateKey = privateKey;
        configuredPublicKey = publicKey;
    }

    private static RSA rsa() {
        return new RSA(
                requireConfig(configuredPrivateKey, "security.config-rsa.private-key", "CONFIG_RSA_PRIVATE_KEY"),
                requireConfig(configuredPublicKey, "security.config-rsa.public-key", "CONFIG_RSA_PUBLIC_KEY")
        );
    }

    private static String requireConfig(String configuredValue, String propertyName, String envName) {
        if (hasText(configuredValue)) {
            return configuredValue.trim();
        }
        String propertyValue = System.getProperty(propertyName);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }
        String envValue = System.getenv(envName);
        if (hasText(envValue)) {
            return envValue.trim();
        }
        throw new IllegalStateException("Configure RSA key is not configured: " + propertyName);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * description: 解密配置中的字符串
     *
     * @param configParam 配置文件中的加密字符串
     * @param beanClass   Bean对象
     * @return T
     * @author bo
     * @date 2023/3/22 3:05 PM
     */
    public static <T> T decryptConfigStr(String configParam, Class<T> beanClass) {
        if (configParam != null && configParam.startsWith(CHUNKED_PREFIX)) {
            return decryptChunkedConfigStr(configParam, beanClass);
        }
        RSA rsa = rsa();
        String decryptStr = rsa.decryptStr(configParam, KeyType.PrivateKey);
        return JSONUtil.toBean(decryptStr, beanClass);
    }

    /**
     * description: 加密bean对象生成字符串
     *
     * @param object Bean对象
     * @return java.lang.String
     * @author bo
     * @date 2023/3/22 3:11 PM
     */
    public static String encryptConfigStr(Object object) {
        byte[] plainBytes = JSON.toJSONString(object).getBytes(StandardCharsets.UTF_8);
        if (plainBytes.length > ENCRYPT_BLOCK_SIZE) {
            return encryptChunkedConfigStr(plainBytes);
        }
        RSA rsa = rsa();
        return rsa.encryptBase64(JSON.toJSONString(object), KeyType.PublicKey);
    }

    private static String encryptChunkedConfigStr(byte[] plainBytes) {
        RSA rsa = rsa();
        StringBuilder encryptConfigStr = new StringBuilder(CHUNKED_PREFIX);
        for (int i = 0; i < plainBytes.length; i += ENCRYPT_BLOCK_SIZE) {
            int blockLength = Math.min(ENCRYPT_BLOCK_SIZE, plainBytes.length - i);
            byte[] block = new byte[blockLength];
            System.arraycopy(plainBytes, i, block, 0, blockLength);
            if (encryptConfigStr.length() > CHUNKED_PREFIX.length()) {
                encryptConfigStr.append(CHUNK_SEPARATOR);
            }
            encryptConfigStr.append(Base64.getEncoder().encodeToString(rsa.encrypt(block, KeyType.PublicKey)));
        }
        return encryptConfigStr.toString();
    }

    private static <T> T decryptChunkedConfigStr(String configParam, Class<T> beanClass) {
        RSA rsa = rsa();
        String payload = configParam.substring(CHUNKED_PREFIX.length());
        String[] encryptedBlocks = payload.split("\\" + CHUNK_SEPARATOR);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (String encryptedBlock : encryptedBlocks) {
            byte[] decryptBlock = rsa.decrypt(Base64.getDecoder().decode(encryptedBlock), KeyType.PrivateKey);
            outputStream.write(decryptBlock, 0, decryptBlock.length);
        }
        String decryptStr = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        return JSONUtil.toBean(decryptStr, beanClass);
    }


    public static void main(String[] args) {
        /*WebsiteBaseConfigVo websiteBaseConfigVo = WebsiteBaseConfigVo.builder()
                .websiteName("括彩CDN")
                .websiteIcon("http://124.222.97.161:9000/kuocaicdn/4960e1df20db6a28144dd0c03d3cd481.png")
                .websiteAnnouncement("括彩云CDN 是在传统 CDN 基础上实现的对数据网络加速进一步优化的智能管理服务。")
                .defaultFlowPrice(BigDecimal.valueOf(2))
                .icpNumber("沪ICP备20021948号-12")
                .defaultAvatar("http://124.222.97.161:9000/kuocaicdn/51b3e925a32bab042c9c799b522ceab3.png")
                .updateTime(DateUtil.now())
                .build();*/
    }

}
