package com.shopwave.exception;

public class OperationTimeoutException extends RuntimeException {

    public OperationTimeoutException(String message) {
        super(message);
    }
}
