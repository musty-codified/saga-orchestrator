package com.mustycodified.musty_process_payment.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookInboxService {
    private static final String CONSUMER_ROUTE = "paystack-webhook";

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    @Qualifier("postgresJdbcTemplate")
    private final JdbcTemplate postgresJdbcTemplate;
    private final TransactionTemplate postgresTransactionTemplate;

    public boolean claim(Map<String, Object> payment) {
        String webhookEventId = String.valueOf(payment.get("id"));
       return Boolean.TRUE.equals(postgresTransactionTemplate.execute(status -> {
           try {
               postgresJdbcTemplate.update(
                       "INSERT INTO inbox (consumer_route, dedup_key) VALUES (?, ?)",
                       CONSUMER_ROUTE, webhookEventId);
               return true;

           } catch (DuplicateKeyException e) {
               status.setRollbackOnly();
               log.info("Duplicate Paystack webhook, skipping → eventId: {}", webhookEventId);
               return false;
           }
       }));
    }

    public boolean verifyPaystackSignature(String rawBody, String signature) {
        if (rawBody == null || signature == null || signature.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paystackSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(hash);
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Paystack signature verification failed", e);
            return false;
        }
    }
}
