package com.shopwave.service;

import com.shopwave.config.TimeoutProperties;
import com.shopwave.exception.OperationTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimeoutGuardService {

    private final TimeoutProperties properties;
    private final ThreadLocal<Long> orderDeadlineAtNanos = new ThreadLocal<>();

    public void startOrderDeadline() {
        long timeoutMs = Math.max(0, properties.getOrderPlacementMs());
        if (timeoutMs <= 0) {
            orderDeadlineAtNanos.remove();
            return;
        }

        orderDeadlineAtNanos.set(System.nanoTime() + toNanos(timeoutMs));
    }

    public void clearOrderDeadline() {
        orderDeadlineAtNanos.remove();
    }

    public void checkOrderDeadline(String stage) {
        Long deadlineAt = orderDeadlineAtNanos.get();
        if (deadlineAt == null) {
            return;
        }

        if (System.nanoTime() > deadlineAt) {
            throw new OperationTimeoutException("Order placement deadline exceeded during " + stage);
        }
    }

    public long startTimer() {
        return System.nanoTime();
    }

    public void checkStockReservationTimeout(long startedAtNanos, Long productId) {
        long timeoutMs = Math.max(0, properties.getStockReservationMs());
        if (timeoutMs <= 0) {
            return;
        }

        long elapsedNanos = System.nanoTime() - startedAtNanos;
        if (elapsedNanos > toNanos(timeoutMs)) {
            long elapsedMs = elapsedNanos / 1_000_000;
            throw new OperationTimeoutException(
                    "Stock reservation timed out for product %d after %d ms".formatted(productId, elapsedMs));
        }
    }

    private long toNanos(long millis) {
        return millis * 1_000_000;
    }
}
