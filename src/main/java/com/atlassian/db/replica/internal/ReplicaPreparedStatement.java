package com.atlassian.db.replica.internal;

import com.atlassian.db.replica.api.reason.Reason;
import com.atlassian.db.replica.api.reason.RouteDecision;
import com.atlassian.db.replica.spi.DatabaseCall;
import com.atlassian.db.replica.spi.ReplicaConsistency;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

import static com.atlassian.db.replica.api.reason.Reason.READ_OPERATION;
import static com.atlassian.db.replica.api.reason.Reason.RW_API_CALL;

public class ReplicaPreparedStatement extends ReplicaStatement implements PreparedStatement {
    private final String sql;
    private final Integer resultSetType;
    private final Integer resultSetConcurrency;
    private final Integer resultSetHoldability;
    private final Integer autoGeneratedKeys;
    private final String[] columnNames;
    private final int[] columnIndexes;

    protected ReplicaPreparedStatement(
        ReplicaConnectionProvider connectionProvider,
        ReplicaConsistency consistency,
        DatabaseCall databaseCall,
        String sql,
        Integer resultSetType,
        Integer resultSetConcurrency,
        Integer resultSetHoldability,
        Integer autoGeneratedKeys,
        String[] columnNames,
        int[] columnIndexes
    ) {
        super(consistency, connectionProvider, databaseCall, resultSetType, resultSetConcurrency, resultSetHoldability);
        this.sql = sql;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.autoGeneratedKeys = autoGeneratedKeys;
        this.columnNames = columnNames;
        this.columnIndexes = columnIndexes;
    }

    protected ReplicaPreparedStatement(
        ReplicaConnectionProvider connectionProvider,
        ReplicaConsistency consistency,
        DatabaseCall databaseCall,
        String sql,
        Integer resultSetType,
        Integer resultSetConcurrency,
        Integer resultSetHoldability
    ) {
        super(consistency, connectionProvider, databaseCall, resultSetType, resultSetConcurrency, resultSetHoldability);
        this.sql = sql;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.autoGeneratedKeys = null;
        this.columnNames = null;
        this.columnIndexes = null;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkClosed();
        final RouteDecision decision = new RouteDecision(READ_OPERATION).withSql(sql);
        final PreparedStatement statement = getReadStatement(decision);
        return execute(statement::executeQuery, decision);
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkClosed();
        final RouteDecision decision = new RouteDecision(RW_API_CALL).withSql(this.sql);
        final PreparedStatement statement = getWriteStatement(decision);
        return execute(statement::executeUpdate, decision);
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        checkClosed();
        final RouteDecision decision = new RouteDecision(RW_API_CALL).withSql(this.sql);
        final PreparedStatement statement = getWriteStatement(decision);
        return execute(statement::executeLargeUpdate, decision);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNull(parameterIndex, sqlType)
        );
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBoolean(parameterIndex, x)
        );
    }

    @Override
    public void setByte(int parameterIndex, byte x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setByte(parameterIndex, x)
        );
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setShort(parameterIndex, x)
        );
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setInt(parameterIndex, x)
        );
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setLong(parameterIndex, x)
        );
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setFloat(parameterIndex, x)
        );
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setDouble(parameterIndex, x)
        );
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBigDecimal(parameterIndex, x)
        );
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setString(parameterIndex, x)
        );
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBytes(parameterIndex, x)
        );
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setDate(parameterIndex, x)
        );
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setTime(parameterIndex, x)
        );
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setTimestamp(parameterIndex, x)
        );
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setAsciiStream(parameterIndex, x)
        );
    }

    @Override
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> {
                //noinspection deprecation
                statement.setUnicodeStream(parameterIndex, x, length);
            }
        );
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBinaryStream(parameterIndex, x, length)
        );
    }

    @Override
    public void clearParameters() throws SQLException {
        checkClosed();
        clearOperations();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setObject(parameterIndex, x, targetSqlType)
        );
    }

    @Override
    public void setObject(int parameterIndex, Object x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setObject(parameterIndex, x)
        );
    }

    @Override
    public boolean execute() throws SQLException {
        checkClosed();
        final RouteDecision decision = new RouteDecision(RW_API_CALL).withSql(this.sql);
        final PreparedStatement statement = getWriteStatement(decision);
        return execute(statement::execute, decision);
    }

    @Override
    public void addBatch() throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) PreparedStatement::addBatch
        );
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setCharacterStream(
                parameterIndex,
                reader,
                length
            )
        );
    }

    @Override
    public void setRef(int parameterIndex, Ref x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setRef(parameterIndex, x)
        );
    }

    @Override
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBlob(parameterIndex, x)
        );
    }

    @Override
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setClob(parameterIndex, x)
        );
    }

    @Override
    public void setArray(int parameterIndex, Array x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setArray(parameterIndex, x)
        );
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setDate(parameterIndex, x)
        );
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setTime(parameterIndex, x)
        );
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setTimestamp(parameterIndex, x, cal)
        );
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNull(parameterIndex, sqlType, typeName)
        );
    }

    @Override
    public void setURL(int parameterIndex, URL x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setURL(parameterIndex, x)
        );
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        checkClosed();
        final PreparedStatement currentStatement = getCurrentStatement();
        if (currentStatement != null) {
            return currentStatement.getParameterMetaData();
        } else {
            return getWriteStatement(new RouteDecision(RW_API_CALL)).getParameterMetaData();
        }
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setRowId(parameterIndex, x)
        );
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNString(parameterIndex, value)
        );
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNCharacterStream(
                parameterIndex,
                value,
                length
            )
        );
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNClob(parameterIndex, value)
        );
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setClob(parameterIndex, reader, length)
        );
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBlob(parameterIndex, inputStream, length)
        );
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNClob(parameterIndex, reader, length)
        );
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setSQLXML(parameterIndex, xmlObject)
        );
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setObject(
                parameterIndex,
                x,
                targetSqlType,
                scaleOrLength
            )
        );
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setAsciiStream(parameterIndex, x, length)
        );
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBinaryStream(parameterIndex, x, length)
        );
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setCharacterStream(
                parameterIndex,
                reader,
                length
            )
        );
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setAsciiStream(parameterIndex, x)
        );
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBinaryStream(parameterIndex, x)
        );
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setCharacterStream(parameterIndex, reader)
        );
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNCharacterStream(parameterIndex, value)
        );
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setClob(parameterIndex, reader)
        );
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setBlob(parameterIndex, inputStream)
        );
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<PreparedStatement>) statement -> statement.setNClob(parameterIndex, reader)
        );
    }

    @Override
    public PreparedStatement getWriteStatement(RouteDecision decision) {
        return (PreparedStatement) super.getWriteStatement(decision);
    }

    @Override
    public PreparedStatement getReadStatement(RouteDecision decision) {
        return (PreparedStatement) super.getReadStatement(decision);
    }

    @Override
    public PreparedStatement getCurrentStatement() {
        return (PreparedStatement) super.getCurrentStatement();
    }

    public PreparedStatement createStatement(Connection connection) throws SQLException {
        if (columnIndexes != null) {
            return connection.prepareStatement(sql, columnIndexes);
        } else if (columnNames != null) {
            return connection.prepareStatement(sql, columnNames);
        } else if (autoGeneratedKeys != null) {
            return connection.prepareStatement(sql, autoGeneratedKeys);
        } else if (resultSetType == null) {
            return connection.prepareStatement(sql);
        } else if (resultSetHoldability == null) {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        } else {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
    }

    public static class Builder {
        private final ReplicaConnectionProvider connectionProvider;
        private final ReplicaConsistency consistency;
        private final DatabaseCall databaseCall;
        private final String sql;
        private Integer resultSetType;
        private Integer resultSetConcurrency;
        private Integer resultSetHoldability;
        private Integer autoGeneratedKeys;
        private String[] columnNames;
        private int[] columnIndexes;

        public Builder(
            ReplicaConnectionProvider connectionProvider,
            ReplicaConsistency consistency,
            DatabaseCall databaseCall,
            String sql
        ) {
            this.connectionProvider = connectionProvider;
            this.consistency = consistency;
            this.databaseCall = databaseCall;
            this.sql = sql;
        }

        public ReplicaPreparedStatement.Builder resultSetType(int resultSetType) {
            this.resultSetType = resultSetType;
            return this;
        }

        public ReplicaPreparedStatement.Builder resultSetConcurrency(int resultSetConcurrency) {
            this.resultSetConcurrency = resultSetConcurrency;
            return this;
        }

        public ReplicaPreparedStatement.Builder resultSetHoldability(int resultSetHoldability) {
            this.resultSetHoldability = resultSetHoldability;
            return this;
        }

        public ReplicaPreparedStatement.Builder autoGeneratedKeys(int autoGeneratedKeys) {
            this.autoGeneratedKeys = autoGeneratedKeys;
            return this;
        }

        public ReplicaPreparedStatement.Builder columnNames(String[] columnNames) {
            this.columnNames = columnNames;
            return this;
        }

        public ReplicaPreparedStatement.Builder columnIndexes(int[] columnIndexes) {
            this.columnIndexes = columnIndexes;
            return this;
        }

        public ReplicaPreparedStatement build() {
            return new ReplicaPreparedStatement(
                connectionProvider,
                consistency,
                databaseCall,
                sql,
                resultSetType,
                resultSetConcurrency,
                resultSetHoldability,
                autoGeneratedKeys,
                columnNames,
                columnIndexes
            );
        }
    }
}
