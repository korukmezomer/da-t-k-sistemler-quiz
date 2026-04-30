package com.shopwave.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopwave.config.IdempotencyProperties;
import com.shopwave.dto.OrderDto;
import com.shopwave.dto.PlaceOrderRequest;
import com.shopwave.exception.IdempotencyConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CachedOrderResult> cache = new ConcurrentHashMap<>();

    public OrderDto executeOrderCreation(String idempotencyKey,
                                         PlaceOrderRequest request,
                                         Supplier<OrderDto> action) {
        if (!properties.isEnabled() || idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String normalizedKey = idempotencyKey.trim();
        String fingerprint = fingerprintOf(request);
        long expiresAtEpochMilli = Instant.now().toEpochMilli() + Math.max(1, properties.getTtlSeconds()) * 1000;

        while (true) {
            CachedOrderResult existing = cache.get(normalizedKey);
            if (existing != null) {
                if (existing.isExpired()) {
                    cache.remove(normalizedKey, existing);
                } else {
                    ensureSameFingerprint(existing, fingerprint);
                    return waitForResult(existing);
                }
            }

            CachedOrderResult candidate = new CachedOrderResult(fingerprint, expiresAtEpochMilli);
            CachedOrderResult previous = cache.putIfAbsent(normalizedKey, candidate);
            if (previous != null) {
                continue;
            }

            try {
                OrderDto response = action.get();
                candidate.complete(response);
                return response;
            } catch (RuntimeException ex) {
                cache.remove(normalizedKey, candidate);
                candidate.fail(ex);
                throw ex;
            }
        }
    }

    private void ensureSameFingerprint(CachedOrderResult cachedResult, String fingerprint) {
        if (!cachedResult.fingerprint.equals(fingerprint)) {
            throw new IdempotencyConflictException(
                    "Idempotency key was already used with a different request payload");
        }
    }

    private OrderDto waitForResult(CachedOrderResult cachedResult) {
        try {
            return cachedResult.future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for idempotent result", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Idempotent execution failed", cause);
        }
    }

    private String fingerprintOf(PlaceOrderRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize request for idempotency", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static final class CachedOrderResult {
        private final String fingerprint;
        private final long expiresAtEpochMilli;
        private final CompletableFuture<OrderDto> future = new CompletableFuture<>();

        private CachedOrderResult(String fingerprint, long expiresAtEpochMilli) {
            this.fingerprint = fingerprint;
            this.expiresAtEpochMilli = expiresAtEpochMilli;
        }

        private boolean isExpired() {
            return Instant.now().toEpochMilli() > expiresAtEpochMilli;
        }

        private void complete(OrderDto response) {
            future.complete(response);
        }

        private void fail(RuntimeException ex) {
            future.completeExceptionally(ex);
        }
    }
}
