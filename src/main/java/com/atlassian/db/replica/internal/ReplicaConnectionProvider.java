package com.atlassian.db.replica.internal;

import com.atlassian.db.replica.api.ConnectionProvider;
import com.atlassian.db.replica.api.ReplicaConsistency;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReplicaConnectionProvider implements AutoCloseable {
    private final ReplicaConsistency consistency;
    private final ConnectionProvider connectionProvider;
    private final Set<Connection> initializedConnections = new HashSet<>();
    private Boolean isAutoCommit;
    private Integer transactionIsolation;
    private Boolean isReadOnly;
    private String catalog;
    private SQLWarning warning;
    private Map<String, Class<?>> typeMap;
    private Integer holdability;
    private volatile Boolean isClosed = false;
    private final LazyReference<Connection> readConnection = new LazyReference<Connection>() {
        @Override
        protected Connection create() throws SQLException {
            if (connectionProvider.isReplicaAvailable()) {
                return connectionProvider.getReplicaConnection();
            } else {
                return writeConnection.get();
            }
        }
    };

    private final LazyReference<Connection> writeConnection = new LazyReference<Connection>() {
        @Override
        protected Connection create() throws SQLException {
            return connectionProvider.getMainConnection();
        }
    };

    public ReplicaConnectionProvider(
        ConnectionProvider connectionProvider,
        ReplicaConsistency consistency
    ) {
        this.connectionProvider = connectionProvider;
        this.consistency = consistency;
    }

    private void initialize(Connection connection) throws SQLException {
        if (!initializedConnections.contains(connection)) {
            if (isAutoCommit != null) {
                connection.setAutoCommit(isAutoCommit);
            }
            if (transactionIsolation != null) {
                connection.setTransactionIsolation(transactionIsolation);
            }
            if (catalog != null) {
                connection.setCatalog(catalog);
            }
            if (typeMap != null) {
                connection.setTypeMap(typeMap);
            }
            if (holdability != null) {
                connection.setHoldability(holdability);
            }
            initializedConnections.add(connection);
        }
    }

    public void setTransactionIsolation(Integer transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
        initializedConnections.clear();
    }

    public int getTransactionIsolation() throws SQLException {
        if (this.transactionIsolation != null) {
            return this.transactionIsolation;
        } else {
            return getWriteConnection().getTransactionIsolation();
        }
    }

    public void setAutoCommit(Boolean autoCommit) {
        final Boolean autoCommitBefore = getAutoCommit();
        this.isAutoCommit = autoCommit;
        initializedConnections.clear();
        if (!autoCommitBefore.equals(getAutoCommit())) {
            recordCommit(autoCommitBefore);
        }
    }

    public boolean getAutoCommit() {
        return isAutoCommit == null || isAutoCommit;
    }

    public Boolean isClosed() {
        return this.isClosed;
    }

    public boolean getReadOnly() {
        return isReadOnly != null && isReadOnly;
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        isReadOnly = readOnly;
        if (readOnly) {
            getReadConnection().setReadOnly(isReadOnly);
        } else {
            getWriteConnection().setReadOnly(isReadOnly);
        }
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public Map<String, Class<?>> getTypeMap() {
        return typeMap == null ? Collections.emptyMap() : new HashMap<>(typeMap);
    }

    public void setTypeMap(Map<String, Class<?>> typeMap) {
        this.typeMap = typeMap;
    }

    public Integer getHoldability() throws SQLException {
        return holdability == null ? getWriteConnection().getHoldability() : holdability;
    }

    public void setHoldability(Integer holdability) {
        this.holdability = holdability;
    }

    public SQLWarning getWarning() throws SQLException {
        if (this.writeConnection.isInitialized()) {
            final Connection writeConnection = this.writeConnection.get();
            saveWarning(writeConnection.getWarnings());

        }
        if (this.readConnection.isInitialized()) {
            final Connection readConnection = this.readConnection.get();
            saveWarning(readConnection.getWarnings());
        }
        return warning;
    }

    public void clearWarnings() throws SQLException {
        if (writeConnection.isInitialized()) {
            writeConnection.get().clearWarnings();

        }
        if (readConnection.isInitialized()) {
            readConnection.get().clearWarnings();
        }
        warning = null;
    }

    /**
     * Provides a connection that will be used for reading operation. Will use read-replica if possible.
     */
    public Connection getReadConnection() throws SQLException {
        if (transactionIsolation != null && transactionIsolation > Connection.TRANSACTION_READ_COMMITTED) {
            return getWriteConnection();
        }
        if (writeConnection.isInitialized()) {
            return writeConnection.get();
        }
        if (consistency.isConsistent(readConnection)) {
            initialize(readConnection.get());
            return readConnection.get();
        } else {
            return getWriteConnection();
        }
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        final Connection currentConnection = readConnection.isInitialized() ? readConnection.get() : writeConnection.get();
        if (iface.isAssignableFrom(currentConnection.getClass())) {
            return iface.cast(currentConnection);
        } else {
            return currentConnection.unwrap(iface);
        }
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        final Connection currentConnection = readConnection.isInitialized() ? readConnection.get() : writeConnection.get();
        if (iface.isAssignableFrom(currentConnection.getClass())) {
            return true;
        } else {
            return currentConnection.isWrapperFor(iface);
        }
    }

    /**
     * Provides a connection that will be used for writing operation. It will always return a connection to the
     * main database.
     */
    public Connection getWriteConnection() throws SQLException {
        final Connection connection = writeConnection.get();
        if (readConnection.isInitialized() && !readConnection.get().equals(writeConnection.get())) {
            closeConnection(readConnection);
        }
        initialize(connection);
        return connection;
    }

    public boolean hasWriteConnection() {
        return writeConnection.isInitialized();
    }

    public void rollback() throws SQLException {
        if (writeConnection.isInitialized()) {
            writeConnection.get().rollback();
        }
        if (readConnection.isInitialized()) {
            if (writeConnection.isInitialized() && readConnection.get().equals(writeConnection.get())) {
                return;
            }
            readConnection.get().rollback();
        }
    }

    public void commit() throws SQLException {
        if (writeConnection.isInitialized()) {
            writeConnection.get().commit();
            recordCommit(isAutoCommit);
        }
        if (readConnection.isInitialized()) {
            if (writeConnection.isInitialized() && readConnection.get().equals(writeConnection.get())) {
                return;
            }
            readConnection.get().commit();
        }
    }

    private void recordCommit(Boolean autoCommit) {
        if (writeConnection.isInitialized() && autoCommit != null && !autoCommit) {
            consistency.write(writeConnection.get());
        }
    }

    @Override
    public void close() throws SQLException {
        Exception lastException = null;
        isClosed = true;
        if (readConnection.isInitialized()) {
            final boolean isWriteAndReadTheSameConnection = writeConnection.isInitialized() && readConnection.get().equals(
                writeConnection.get());
            try {
                closeConnection(readConnection);
            } catch (Exception e) {
                lastException = e;
            }
            if (isWriteAndReadTheSameConnection) {
                writeConnection.reset();
                if (lastException != null) {
                    throw new SQLException(lastException);
                }
                return;
            }
        }
        if (writeConnection.isInitialized()) {
            try {
                closeConnection(writeConnection);
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (lastException != null) {
            throw new SQLException(lastException);
        }
    }

    private void closeConnection(LazyReference<Connection> connectionReference) throws SQLException {
        try {
            final Connection connection = connectionReference.get();
            try {
                saveWarning(connection.getWarnings());
            } catch (Exception e) {
                saveWarning(new SQLWarning(e));
            }
            connection.close();
        } finally {
            connectionReference.reset();
        }
    }

    private void saveWarning(SQLWarning warning) {
        if (warning == null || isLastWarning(warning)) {
            return;
        }
        if (this.warning == null) {
            this.warning = warning;
        } else {
            this.warning.setNextWarning(warning);
        }
    }

    private boolean isLastWarning(SQLWarning warning) {
        if (this.warning == null) {
            return false;
        }
        SQLWarning lastWarning = this.warning;
        for (int i = 0; i < 100; i++) {
            if (lastWarning.getNextWarning() == null) {
                return lastWarning.equals(warning);
            } else
                lastWarning = lastWarning.getNextWarning();
        }
        return true;
    }
}
