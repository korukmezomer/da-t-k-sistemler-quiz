package com.shopwave.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shopwave.timeout")
public class TimeoutProperties {

    private long stockReservationMs = 2000;
    private long orderPlacementMs = 5000;

    public long getStockReservationMs() {
        return stockReservationMs;
    }

    public void setStockReservationMs(long stockReservationMs) {
        this.stockReservationMs = stockReservationMs;
    }

    public long getOrderPlacementMs() {
        return orderPlacementMs;
    }

    public void setOrderPlacementMs(long orderPlacementMs) {
        this.orderPlacementMs = orderPlacementMs;
    }
}
