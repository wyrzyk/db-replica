package com.atlassian.db.replica.api.circuitbreaker;

import com.atlassian.db.replica.api.circuitbreaker.BreakerState;

public interface CircuitBreaker {
    BreakerState getState();

    void handle(Throwable throwable);
}
