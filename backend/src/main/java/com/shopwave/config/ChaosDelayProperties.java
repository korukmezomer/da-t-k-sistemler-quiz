package com.shopwave.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@ConfigurationProperties(prefix = "shopwave.chaos.delay")
public class ChaosDelayProperties {

    private boolean enabled;
    private long fixedMs;
    private long jitterMs;
    private Set<String> targetOperations = new LinkedHashSet<>(Set.of("placeorder", "reservestock"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getFixedMs() {
        return fixedMs;
    }

    public void setFixedMs(long fixedMs) {
        this.fixedMs = fixedMs;
    }

    public long getJitterMs() {
        return jitterMs;
    }

    public void setJitterMs(long jitterMs) {
        this.jitterMs = jitterMs;
    }

    public Set<String> getTargetOperations() {
        return targetOperations;
    }

    public void setTargetOperations(Set<String> targetOperations) {
        this.targetOperations = normalize(targetOperations);
    }

    public boolean targets(String operation) {
        return normalize(operation) != null && targetOperations.contains(normalize(operation));
    }

    private Set<String> normalize(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return new LinkedHashSet<>();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String normalizedValue = normalize(value);
            if (normalizedValue != null) {
                normalized.add(normalizedValue);
            }
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
