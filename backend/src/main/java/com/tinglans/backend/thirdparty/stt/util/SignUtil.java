package com.tinglans.backend.thirdparty.stt.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 科大讯飞签名工具类
 */
@Slf4j
public class SignUtil {

    private static final String HMAC_SHA256 = "hmac-sha256";
    private static final String SHA256 = "SHA-256";

    /**
     * 生成RFC1123格式的日期
     */
    public static String getRFC1123Date() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }

    /**
     * 生成Digest值（空body的情况）
     */
    public static String getDigest() {
        try {
            byte[] hash = MessageDigest.getInstance(SHA256).digest(new byte[0]);
            return "SHA-256=" + Base64.encodeBase64String(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("生成Digest失败", e);
            throw new RuntimeException("生成Digest失败", e);
        }
    }

    /**
     * 生成请求头
     *
     * @param method     HTTP方法（如POST）
     * @param path       请求路径（如/file/upload）
     * @param host       主机名（如upload-ost-api.xfyun.cn）
     * @param apiKey     API密钥
     * @param apiSecret  API密钥Secret
     * @return 请求头Map
     */
    public static Map<String, String> generateHeaders(String method, String path, String host,
                                                       String apiKey, String apiSecret) {
        Map<String, String> headers = new HashMap<>();

        // 生成date
        String date = getRFC1123Date();

        // 生成digest
        String digest = getDigest();

        // 生成signature
        String signature = generateSignature(method, path, host, date, digest, apiSecret);

        // 生成authorization
        String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"host date request-line digest\", signature=\"%s\"",
                apiKey, HMAC_SHA256, signature
        );

        headers.put("host", host);
        headers.put("date", date);
        headers.put("digest", digest);
        headers.put("authorization", authorization);

        return headers;
    }

    /**
     * 生成签名
     */
    private static String generateSignature(String method, String path, String host,
                                             String date, String digest, String apiSecret) {
        // 构建签名原文
        String requestLine = method + " " + path + " HTTP/1.1";
        String signatureOrigin = "host: " + host + "\n"
                + "date: " + date + "\n"
                + requestLine + "\n"
                + "digest: " + digest;

        log.debug("签名原文: {}", signatureOrigin);

        try {
            // HMAC-SHA256加密
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    apiSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] signatureSha = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));

            // Base64编码
            return Base64.encodeBase64String(signatureSha);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
}
