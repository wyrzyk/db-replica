package com.atlassian.db.replica.api.mocks;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;

class ConnectionMock implements Connection {

    @Override
    public Statement createStatement() throws SQLException {
        return mock(Statement.class);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return mock(PreparedStatement.class);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return mock(CallableStatement.class);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void commit() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void rollback() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void close() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public boolean isClosed() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public String getCatalog() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return mock(Statement.class);
    }

    @Override
    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency
    ) throws SQLException {
        return mock(PreparedStatement.class);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return mock(CallableStatement.class);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Statement createStatement(
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability
    ) throws SQLException {
        return mock(Statement.class);
    }

    @Override
    public PreparedStatement prepareStatement(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability
    ) throws SQLException {
        return mock(PreparedStatement.class);
    }

    @Override
    public CallableStatement prepareCall(
        String sql,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability
    ) throws SQLException {
        return mock(CallableStatement.class);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return mock(PreparedStatement.class);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return mock(PreparedStatement.class);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return mock(PreparedStatement.class);
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        throw new RuntimeException();
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        throw new RuntimeException();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public String getSchema() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new RuntimeException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new RuntimeException();
    }
}