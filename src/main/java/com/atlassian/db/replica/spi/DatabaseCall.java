package com.atlassian.db.replica.spi;

import com.atlassian.db.replica.api.SqlCall;
import com.atlassian.db.replica.api.context.QueryContext;

import java.sql.SQLException;

/**
 * Intercepts call to a database.
 */
public interface DatabaseCall {
    <T> T call(final SqlCall<T> call, QueryContext context) throws SQLException;
}
