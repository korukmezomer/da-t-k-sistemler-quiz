package com.shopwave.service;

import com.shopwave.config.ChaosDelayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChaosDelayService {

    private final ChaosDelayProperties properties;

    public void injectDelay(String operation) {
        if (!properties.isEnabled() || !properties.targets(operation)) {
            return;
        }

        long fixedDelayMs = Math.max(0, properties.getFixedMs());
        long jitterDelayMs = properties.getJitterMs() > 0
                ? ThreadLocalRandom.current().nextLong(properties.getJitterMs() + 1)
                : 0;
        long totalDelayMs = fixedDelayMs + jitterDelayMs;

        if (totalDelayMs <= 0) {
            return;
        }

        log.info("Injecting chaos delay operation={} delayMs={} fixedMs={} jitterMs={}",
                operation, totalDelayMs, fixedDelayMs, jitterDelayMs);

        try {
            Thread.sleep(totalDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chaos delay interrupted for operation: " + operation, ex);
        }
    }
}
