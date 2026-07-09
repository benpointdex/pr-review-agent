package com.henry.git_pr.service;

import com.henry.git_pr.exception.WebhookVerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class WebhookSignatureService {

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    /**
     * Verifies that the incoming GitHub webhook payload is authentic.
     *
     * GitHub hashes the payload body using HMAC-SHA256 and your secret key,
     * and sends the result in the 'X-Hub-Signature-256' header prefixed with 'sha256='.
     */
    public boolean isValidSignature(String rawPayload, String signatureHeader) {
        try {
            if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
//                log.warn("Invalid signature header format");
                throw new WebhookVerificationException("Invalid signature header format");

            }

            // Extract the hex hash part after "sha256="
            String receivedHex = signatureHeader.substring(7);

            // Compute the expected HMAC-SHA256 hash using your webhook secret
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));

            // Convert raw bytes to hex string format
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : rawHmac) {
                hexBuilder.append(String.format("%02x", b));
            }
            String expectedHex = hexBuilder.toString();

            // Compare using constant-time comparison to prevent timing attacks
            boolean valid = constantTimeEquals(expectedHex, receivedHex);
            if (!valid) {
//                log.warn("Signature mismatch. Request might be forged.");
                throw new WebhookVerificationException("Signature mismatch. Request might be forged.");
            }
            return true;

        } catch (WebhookVerificationException e) {
            throw e;
        } catch (Exception ex){
            log.error("Crypto error occurred while validating signature", ex);
            return false;
        }
    }

    /**
     * Simple constant-time comparison to protect against timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
