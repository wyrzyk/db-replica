package com.atlassian.db.replica.internal.circuitbreaker;

import com.atlassian.db.replica.api.SqlCall;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLWarning;
import java.sql.Statement;

public class BreakerStatement implements Statement {
    private final Statement delegate;
    private final BreakerHandler breakerHandler;

    public BreakerStatement(Statement delegate, BreakerHandler breakerHandler) {
        this.delegate = delegate;
        this.breakerHandler = breakerHandler;
    }

    @Override
    public ResultSet executeQuery(String sql) {
        return breakerHandler.handle(() -> delegate.executeQuery(sql));
    }

    @Override
    public int executeUpdate(String sql) {
        return breakerHandler.handle(() -> delegate.executeUpdate(sql));
    }

    @Override
    public void close() {
        breakerHandler.handle(delegate::close);
    }

    @Override
    public int getMaxFieldSize() {
        return breakerHandler.handle(delegate::getMaxFieldSize);
    }

    @Override
    public void setMaxFieldSize(int max) {
        breakerHandler.handle(() -> delegate.setMaxFieldSize(max));
    }

    @Override
    public int getMaxRows() {
        return breakerHandler.handle(delegate::getMaxRows);
    }

    @Override
    public void setMaxRows(int max) {
        breakerHandler.handle(() -> delegate.setMaxRows(max));
    }

    @Override
    public void setEscapeProcessing(boolean enable) {
        breakerHandler.handle(() -> delegate.setEscapeProcessing(enable));
    }

    @Override
    public int getQueryTimeout() {
        return breakerHandler.handle(delegate::getQueryTimeout);
    }

    @Override
    public void setQueryTimeout(int seconds) {
        breakerHandler.handle(() -> delegate.setQueryTimeout(seconds));
    }

    @Override
    public void cancel() {
        breakerHandler.handle(delegate::cancel);
    }

    @Override
    public SQLWarning getWarnings() {
        return breakerHandler.handle(delegate::getWarnings);
    }

    @Override
    public void clearWarnings() {
        breakerHandler.handle(delegate::clearWarnings);
    }

    @Override
    public void setCursorName(String name) {
        breakerHandler.handle(() -> delegate.setCursorName(name));
    }

    @Override
    public boolean execute(String sql) {
        return breakerHandler.handle(() -> delegate.execute(sql));
    }

    @Override
    public ResultSet getResultSet() {
        return breakerHandler.handle(delegate::getResultSet);
    }

    @Override
    public int getUpdateCount() {
        return breakerHandler.handle(delegate::getUpdateCount);
    }

    @Override
    public boolean getMoreResults() {
        return breakerHandler.handle((SqlCall<Boolean>) delegate::getMoreResults);
    }

    @Override
    public void setFetchDirection(int direction) {
        breakerHandler.handle(() -> delegate.setFetchDirection(direction));
    }

    @Override
    public int getFetchDirection() {
        //noinspection MagicConstant
        return breakerHandler.handle(delegate::getFetchDirection);
    }

    @Override
    public void setFetchSize(int rows) {
        breakerHandler.handle(() -> delegate.setFetchSize(rows));
    }

    @Override
    public int getFetchSize() {
        return breakerHandler.handle(delegate::getFetchSize);
    }

    @Override
    public int getResultSetConcurrency() {
        //noinspection MagicConstant
        return breakerHandler.handle(delegate::getResultSetConcurrency);
    }

    @Override
    public int getResultSetType() {
        //noinspection MagicConstant
        return breakerHandler.handle(delegate::getResultSetType);
    }

    @Override
    public void addBatch(String sql) {
        breakerHandler.handle(() -> delegate.addBatch(sql));
    }

    @Override
    public void clearBatch() {
        breakerHandler.handle(delegate::clearBatch);
    }

    @Override
    public int[] executeBatch() {
        return breakerHandler.handle(delegate::executeBatch);
    }

    @Override
    public Connection getConnection() {
        return breakerHandler.handle(delegate::getConnection);
    }

    @Override
    public boolean getMoreResults(int current) {
        return breakerHandler.handle(() -> delegate.getMoreResults(current));
    }

    @Override
    public ResultSet getGeneratedKeys() {
        return breakerHandler.handle(delegate::getGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) {
        return breakerHandler.handle(() -> delegate.executeUpdate(sql, autoGeneratedKeys));
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) {
        return breakerHandler.handle(() -> delegate.executeUpdate(sql, columnIndexes));
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) {
        return breakerHandler.handle(() -> delegate.executeUpdate(sql, columnNames));
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) {
        return breakerHandler.handle(() -> delegate.execute(sql, autoGeneratedKeys));
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) {
        return breakerHandler.handle(() -> delegate.execute(sql, columnIndexes));
    }

    @Override
    public boolean execute(String sql, String[] columnNames) {
        return breakerHandler.handle(() -> delegate.execute(sql, columnNames));
    }

    @Override
    public int getResultSetHoldability() {
        return breakerHandler.handle(delegate::getResultSetHoldability);
    }

    @Override
    public boolean isClosed() {
        return breakerHandler.handle(delegate::isClosed);
    }

    @Override
    public void setPoolable(@SuppressWarnings("SpellCheckingInspection") boolean poolable) {
        breakerHandler.handle(() -> delegate.setPoolable(poolable));
    }

    @Override
    public boolean isPoolable() {
        return breakerHandler.handle(delegate::isPoolable);
    }

    @Override
    public void closeOnCompletion() {
        breakerHandler.handle(delegate::closeOnCompletion);
    }

    @Override
    public boolean isCloseOnCompletion() {
        return breakerHandler.handle(delegate::isCloseOnCompletion);
    }

    @Override
    public <T> T unwrap(Class<T> iface) {
        return breakerHandler.handle(() -> delegate.unwrap(iface));
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return breakerHandler.handle(() -> delegate.isWrapperFor(iface));
    }

    @Override
    public long getLargeUpdateCount() {
        return breakerHandler.handle(delegate::getLargeUpdateCount);
    }

    @Override
    public void setLargeMaxRows(long max) {
        breakerHandler.handle(() -> delegate.setLargeMaxRows(max));
    }

    @Override
    public long getLargeMaxRows() {
        return breakerHandler.handle(delegate::getLargeMaxRows);
    }

    @Override
    public long[] executeLargeBatch() {
        return breakerHandler.handle(delegate::executeLargeBatch);
    }

    @Override
    public long executeLargeUpdate(String sql) {
        return breakerHandler.handle(() -> delegate.executeLargeUpdate(sql));
    }

    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys) {
        return breakerHandler.handle(() -> delegate.executeLargeUpdate(sql, autoGeneratedKeys));
    }

    @Override
    public long executeLargeUpdate(String sql, int[] columnIndexes) {
        return breakerHandler.handle(() -> delegate.executeLargeUpdate(sql, columnIndexes));
    }

    @Override
    public long executeLargeUpdate(String sql, String[] columnNames) {
        return breakerHandler.handle(() -> delegate.executeLargeUpdate(sql, columnNames));
    }
}
