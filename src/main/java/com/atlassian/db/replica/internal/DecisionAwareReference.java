package com.atlassian.db.replica.internal;

import com.atlassian.db.replica.api.reason.RouteDecision;

import java.util.concurrent.atomic.AtomicReference;

public abstract class DecisionAwareReference<T> extends LazyReference<T> {
    private final AtomicReference<RouteDecision> firstCause = new AtomicReference<>();

    public T get(RouteDecision currentCause) {
        firstCause.compareAndSet(null, currentCause);
        return super.get();
    }

    @Override
    public void reset() {
        super.reset();
        firstCause.set(null);
    }

    public RouteDecision getFirstCause() {
        if (firstCause.get() == null) {
            throw new IllegalStateException("The first cause is unknown");
        }
        return firstCause.get();
    }
}
