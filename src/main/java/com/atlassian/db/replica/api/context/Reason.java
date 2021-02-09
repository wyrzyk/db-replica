package com.atlassian.db.replica.api.context;

import java.util.Objects;

public final class Reason {
    private final String value;

    private Reason(final String value) {
        this.value = value;
    }

    public static final Reason API_CALL = new Reason("API_CALL");
    public static final Reason REPLICA_INCONSISTENT = new Reason("REPLICA_INCONSISTENT");
    public static final Reason READ_OPERATION = new Reason("READ_OPERATION");
    public static final Reason WRITE_OPERATION = new Reason("WRITE_OPERATION");
    public static final Reason LOCK = new Reason("LOCK");
    public static final Reason MAIN_CONNECTION_REUSE = new Reason("MAIN_CONNECTION_REUSE"); // TODO: Add cause to this reason!! (an sql/reason that is responsible for the switch)
    public static final Reason HIGH_TRANSACTION_ISOLATION_LEVEL = new Reason("HIGH_TRANSACTION_ISOLATION_LEVEL");

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reason reason1 = (Reason) o;
        return Objects.equals(value, reason1.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Reason{" +
            "value='" + value + '\'' +
            '}';
    }
}
