package com.atlassian.db.replica.internal;


import java.util.function.Supplier;

public abstract class LazyReference<T> implements Supplier<T> {
    private T value = null;
    private final Object lock = new Object();

    protected abstract T create() throws Exception;

    public boolean isInitialized() {
        return value != null;
    }

    @Override
    public T get() {
        try {
            synchronized (lock) {
                if (!isInitialized()) {
                    value = create();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    public void reset() {
        value = null;
    }

}
