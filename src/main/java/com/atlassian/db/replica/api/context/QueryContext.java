package com.atlassian.db.replica.api.context;


import java.util.Objects;

public final class QueryContext {
    private final Reason reason;
    private final String sql;
    private final boolean runOnMain;


    private QueryContext(Reason reason, String sql, boolean runOnMain) {
        this.reason = reason;
        this.runOnMain = runOnMain;
        this.sql = sql;
    }

    public Reason getReason() {
        return reason;
    }

    public String getSql() {
        return sql;
    }

    public boolean isRunOnMain() {
        return runOnMain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryContext that = (QueryContext) o;
        return runOnMain == that.runOnMain && Objects.equals(reason, that.reason) && Objects.equals(sql, that.sql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reason, sql, runOnMain);
    }

    @Override
    public String toString() {
        return "QueryContext{" +
            "reason=" + reason +
            ", sql='" + sql + '\'' +
            ", runOnMain=" + runOnMain +
            '}';
    }

    public static QueryContext.Builder builder(Reason reason, boolean runOnMain) {
        return new Builder(reason, runOnMain);
    }

    public static final class Builder {
        private Reason reason;
        private boolean runOnMain;
        private String sql;

        public Builder(Reason reason, boolean runOnMain) {
            this.reason = reason;
            this.runOnMain = runOnMain;
        }

        public QueryContext.Builder reason(final Reason reason) {
            this.reason = reason;
            return this;
        }

        public QueryContext.Builder runOnMain(final boolean runOnMain) {
            this.runOnMain = runOnMain;
            return this;
        }

        public QueryContext.Builder sql(final String sql) {
            this.sql = sql;
            return this;
        }

        public QueryContext build() {
            return new QueryContext(
                reason, sql, runOnMain
            );
        }
    }
}
