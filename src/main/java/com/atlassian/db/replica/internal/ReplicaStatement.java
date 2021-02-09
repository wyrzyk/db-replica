package com.atlassian.db.replica.internal;

import com.atlassian.db.replica.api.SqlCall;
import com.atlassian.db.replica.api.context.QueryContext;
import com.atlassian.db.replica.spi.DatabaseCall;
import com.atlassian.db.replica.spi.ReplicaConsistency;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.db.replica.api.context.Reason.*;
import static com.atlassian.db.replica.api.state.State.MAIN;
import static java.lang.Math.min;

public class ReplicaStatement implements Statement {
    private final ReplicaConnectionProvider connectionProvider;
    private final Integer resultSetType;
    private final Integer resultSetConcurrency;
    private final Integer resultSetHoldability;
    private Statement currentStatement;
    private volatile boolean isClosed = false;
    @SuppressWarnings("rawtypes")
    private final List<StatementOperation> operations = new ArrayList<>();
    private final List<StatementOperation<Statement>> batches = new ArrayList<>();
    private final ReplicaConsistency consistency;
    private final DatabaseCall databaseCall;
    final String methodBracketStart = Pattern.quote("(");
    private boolean isWriteOperation = true;
    private final ReadStatementReference readStatement = new ReadStatementReference();
    private final LazyReference<Statement> writeStatement = new LazyReference<Statement>() {
        @Override
        protected Statement create() throws Exception {
            return createStatement(connectionProvider.getWriteConnection());
        }
    };

    public ReplicaStatement(
        ReplicaConsistency consistency,
        ReplicaConnectionProvider connectionProvider,
        DatabaseCall databaseCall,
        Integer resultSetType,
        Integer resultSetConcurrency,
        Integer resultSetHoldability
    ) {
        this.consistency = consistency;
        this.connectionProvider = connectionProvider;
        this.databaseCall = databaseCall;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        checkClosed();
        final QueryContext.Builder contextBuilder = QueryContext.builder(READ_OPERATION,false).sql(sql);
        final Statement statement = getReadStatement(sql, contextBuilder);
        return execute(() -> statement.executeQuery(sql), contextBuilder.build());
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeUpdate(sql),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public void close() throws SQLException {
        isClosed = true;
        for (final Statement statement : allStatements()) {
            try {
                statement.close();
            } catch (Exception e) {
                // Ignore. We can't add it to warnings. It's impossible to read them after Statement#close
            }
        }
        readStatement.reset();
        writeStatement.reset();
        currentStatement = null;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        checkClosed();
        return getWriteStatement().getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setMaxFieldSize(max)
        );
    }

    @Override
    public int getMaxRows() throws SQLException {
        checkClosed();
        return getWriteStatement().getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setMaxRows(max)
        );
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setEscapeProcessing(enable)
        );
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setQueryTimeout(seconds)
        );
    }

    @Override
    public void cancel() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkClosed();
        if (getCurrentStatement() == null) {
            return null;
        } else {
            return getCurrentStatement().getWarnings();
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkClosed();
        if (getCurrentStatement() != null) {
            getCurrentStatement().clearWarnings();
        }
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.execute(sql),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        checkClosed();
        if (getCurrentStatement() == null) {
            return null;
        }
        return getCurrentStatement().getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        checkClosed();
        if (getCurrentStatement() == null) {
            return -1;
        }
        return getCurrentStatement().getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        checkClosed();
        if (getCurrentStatement() == null) {
            return false;
        }
        return getCurrentStatement().getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setFetchDirection(direction)
        );
    }

    @Override
    public int getFetchDirection() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        checkClosed();
        addOperation(statement -> statement.setFetchSize(rows));
    }

    @Override
    public int getFetchSize() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public int getResultSetType() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        checkClosed();
        final StatementOperation<Statement> addBatch = statement -> statement.addBatch(sql);
        addOperation(addBatch);
        batches.add(addBatch);
    }

    @Override
    public void clearBatch() throws SQLException {
        checkClosed();
        batches.forEach(operations::remove);
        batches.clear();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            statement::executeBatch,
            QueryContext.builder(
                API_CALL,
                true
            ).build()
        );
    }

    @Override
    public Connection getConnection() throws SQLException {
        checkClosed();
        return connectionProvider.getWriteConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        checkClosed();
        return currentStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        checkClosed();
        return currentStatement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeUpdate(sql, autoGeneratedKeys),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeUpdate(sql, columnIndexes),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeUpdate(sql, columnNames),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.execute(sql, autoGeneratedKeys),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        checkClosed();
        final Statement state = getWriteStatement();
        return execute(
            () -> state.execute(sql, columnIndexes),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.execute(sql, columnNames),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        checkClosed();
        return currentStatement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setPoolable(poolable)
        );
    }

    @Override
    public boolean isPoolable() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        } else if (currentStatement != null && iface.isAssignableFrom(currentStatement.getClass())) {
            return iface.cast(currentStatement);
        } else if (currentStatement != null) {
            return currentStatement.unwrap(iface);
        } else {
            throw new SQLException();
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return true;
        } else if (currentStatement != null && iface.isAssignableFrom(currentStatement.getClass())) {
            return true;
        } else if (currentStatement != null) {
            return currentStatement.isWrapperFor(iface);
        } else {
            return false;
        }
    }

    @Override
    public long getLargeUpdateCount() throws SQLException {
        checkClosed();
        return currentStatement.getLargeUpdateCount();
    }

    @Override
    public void setLargeMaxRows(long max) throws SQLException {
        checkClosed();
        addOperation(
            (StatementOperation<Statement>) statement -> statement.setLargeMaxRows(max)
        );
    }

    @Override
    public long getLargeMaxRows() throws SQLException {
        checkClosed();
        throw new ReadReplicaUnsupportedOperationException();
    }

    @Override
    public long[] executeLargeBatch() throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            statement::executeLargeBatch,
            QueryContext.builder(
                API_CALL,
                true
            ).build()
        );
    }

    @Override
    public long executeLargeUpdate(String sql) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeLargeUpdate(sql),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public long executeLargeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeLargeUpdate(sql, autoGeneratedKeys),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public long executeLargeUpdate(String sql, int[] columnIndexes) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeLargeUpdate(sql, columnIndexes),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    @Override
    public long executeLargeUpdate(String sql, String[] columnNames) throws SQLException {
        checkClosed();
        final Statement statement = getWriteStatement();
        return execute(
            () -> statement.executeLargeUpdate(sql, columnNames),
            QueryContext.builder(
                API_CALL,
                true
            ).sql(sql)
                .build()
        );
    }

    <T> T execute(final SqlCall<T> call, final QueryContext queryContext) throws SQLException {
        final T result = databaseCall.call(call, queryContext);
        if(queryContext.isRunOnMain() && isWriteOperation){
            recordWriteAfterQueryExecution();
        }
        return result;
    }

    public void performOperations() {
        //noinspection rawtypes
        for (StatementOperation operation : operations) {
            try {
                //noinspection unchecked
                operation.accept(getCurrentStatement());
            } catch (Exception e) {
                throw new ReadReplicaUnsupportedOperationException();
            }
        }
        operations.clear();
    }

    protected Statement getCurrentStatement() {
        return this.currentStatement;
    }

    protected void setCurrentStatement(Statement statement) {
        this.currentStatement = statement;
    }

    protected void addOperation(@SuppressWarnings("rawtypes") StatementOperation operation) {
        operations.add(operation);
    }

    protected void clearOperations() {
        operations.clear();
    }

    public static Builder builder(
        ReplicaConnectionProvider connectionProvider,
        ReplicaConsistency consistency,
        DatabaseCall databaseCall
    ) {
        return new Builder(connectionProvider, consistency, databaseCall);
    }

    void recordWriteAfterQueryExecution() throws SQLException {
        final Connection connection = currentStatement.getConnection();
        if (connection.getAutoCommit()) {
            consistency.write(connection);
        }
    }

    public Statement getReadStatement(String sql, QueryContext.Builder contextBuilder) {
        isWriteOperation = isFunctionCall(sql) || isUpdate(sql) || isDelete(sql);
        if (isWriteOperation) {
            contextBuilder.reason(WRITE_OPERATION).runOnMain(true);
            return prepareWriteStatement();
        }
        if (connectionProvider.getState().equals(MAIN)) {
            contextBuilder.reason(MAIN_CONNECTION_REUSE).runOnMain(true);
            return prepareWriteStatement();
        }

        if (isSelectForUpdate(sql)) {
            contextBuilder.reason(LOCK).runOnMain(true);
            return prepareWriteStatement();
        }

        readStatement.setContextBuilder(contextBuilder);
        setCurrentStatement(getCurrentStatement() != null ? getCurrentStatement() : readStatement.get());
        performOperations();
        return getCurrentStatement();
    }

    private boolean isSelectForUpdate(String sql) {
        return sql != null && (sql.endsWith("for update") || sql.endsWith("FOR UPDATE"));
    }

    private boolean isUpdate(String sql) {
        return sql != null && (sql.startsWith("update") || sql.startsWith("UPDATE"));
    }

    private boolean isDelete(String sql) {
        return sql != null && (sql.startsWith("delete") || sql.startsWith("DELETE"));
    }

    private boolean isFunctionCall(String sql) {
        if (sql == null) {
            return false;
        }
        final String mayContainFunction = skipIrrelevantSqlParts(sql);
        final boolean mayBeFunction = mayContainFunction.contains("(");
        if (!mayBeFunction) {
            return false;
        }
        final String potentialMethodName = mayContainFunction.split(methodBracketStart)[0];
        final boolean hasSpaceInPotentialMethodName = potentialMethodName.contains(" ");
        return !hasSpaceInPotentialMethodName;
    }

    /**
     * Skips `SELECT ` at the beginning of the query. Postgres identifiers are limited to
     * 63 characters, so we should be safe to interpret first 80 characters.
     */
    private String skipIrrelevantSqlParts(String sql) {
        return sql.substring(7, min(sql.length(), 80));
    }

    protected Statement getWriteStatement() {
        isWriteOperation = true;
        return prepareWriteStatement();
    }

    private Statement prepareWriteStatement() {
        setCurrentStatement(writeStatement.get());
        performOperations();
        return getCurrentStatement();
    }

    protected Statement createStatement(Connection connection) throws SQLException {
        if (resultSetType == null) {
            return connection.createStatement();
        } else if (resultSetHoldability == null) {
            return connection.createStatement(resultSetType, resultSetConcurrency);
        } else {
            return connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
    }

    private Collection<Statement> allStatements() {
        return Stream.of(readStatement, writeStatement)
            .filter(LazyReference::isInitialized)
            .map(LazyReference::get)
            .collect(Collectors.toList());
    }

    public static class Builder {
        private final ReplicaConnectionProvider connectionProvider;
        private final ReplicaConsistency consistency;
        private final DatabaseCall databaseCall;
        private Integer resultSetType;
        private Integer resultSetConcurrency;
        private Integer resultSetHoldability;

        private Builder(
            ReplicaConnectionProvider connectionProvider,
            ReplicaConsistency consistency,
            DatabaseCall databaseCall
        ) {
            this.connectionProvider = connectionProvider;
            this.consistency = consistency;
            this.databaseCall = databaseCall;
        }

        public Builder resultSetType(int resultSetType) {
            this.resultSetType = resultSetType;
            return this;
        }

        public Builder resultSetConcurrency(int resultSetConcurrency) {
            this.resultSetConcurrency = resultSetConcurrency;
            return this;
        }

        public Builder resultSetHoldability(int resultSetHoldability) {
            this.resultSetHoldability = resultSetHoldability;
            return this;
        }

        public ReplicaStatement build() {
            return new ReplicaStatement(
                consistency,
                connectionProvider,
                databaseCall,
                resultSetType,
                resultSetConcurrency,
                resultSetHoldability
            );
        }
    }

    protected void checkClosed() throws SQLException {
        if (isClosed()) {
            throw new SQLException("This connection has been closed.");
        }
    }

    private final class ReadStatementReference extends LazyReference<Statement> {
        private QueryContext.Builder contextBuilder;

        public void setContextBuilder(QueryContext.Builder contextBuilder) {
            this.contextBuilder = contextBuilder;
        }

        @Override
        protected Statement create() throws Exception {
            final Connection connection = connectionProvider.getReadConnection(contextBuilder);
            return createStatement(connection);
        }
    }
}
