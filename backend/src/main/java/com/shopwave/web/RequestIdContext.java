package com.shopwave.web;

import org.slf4j.MDC;

public final class RequestIdContext {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "requestId";

    private RequestIdContext() {
    }

    public static String getCurrentRequestId() {
        return MDC.get(MDC_KEY);
    }
}
