package com.delma.paymentservice.entity;

public enum IdempotencyStatus {
    PROCESSING,   // request in flight — concurrent duplicate should wait
    COMPLETED,    // success — return cached response
    FAILED        // failed — allow retry

}
