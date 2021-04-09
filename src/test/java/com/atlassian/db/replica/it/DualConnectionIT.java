package com.atlassian.db.replica.it;

import com.atlassian.db.replica.SequenceReplicaConsistency;
import com.atlassian.db.replica.api.DualConnection;
import com.atlassian.db.replica.api.PessimisticPropagationConsistency;
import com.atlassian.db.replica.api.mocks.CircularConsistency;
import com.atlassian.db.replica.internal.LsnReplicaConsistency10;
import com.atlassian.db.replica.internal.LsnReplicaConsistency9;
import com.atlassian.db.replica.it.consistency.WaitingReplicaConsistency;
import com.atlassian.db.replica.spi.Cache;
import com.atlassian.db.replica.spi.ReplicaConsistency;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.postgresql.jdbc.PgConnection;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.*;

import static com.atlassian.db.replica.api.Queries.SIMPLE_QUERY;
import static java.sql.ResultSet.*;
import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.assertj.core.api.Assertions.*;

public class DualConnectionIT {

    @Test
    public void shouldUseReplica() throws SQLException {
        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            Connection connection = DualConnection.builder(connectionProvider, new LsnReplicaConsistency10()).build();

            try (final ResultSet resultSet = connection.prepareStatement("SELECT 1;").executeQuery()) {
                resultSet.next();
                assertThat(resultSet.getLong(1)).isEqualTo(1);
            }
        }
    }


    @Test
    public void shouldPreserveAutoCommitModeWhileSwitchingFromMainToReplica() throws SQLException {
        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            final Connection connection = DualConnection.builder(
                connectionProvider,
                new CircularConsistency.Builder(ImmutableList.of(false, true)).build()
            ).build();

            connection.setAutoCommit(false);
            connection.prepareStatement("SELECT 1;").executeQuery();
            connection.prepareStatement("SELECT 1;").executeQuery();
            connection.commit();
        }
    }

    @Test
    public void shouldPreserveReadOnlyModeWhileSwitchingFromReplicaToMain() throws SQLException {
        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            final WaitingReplicaConsistency consistency = new WaitingReplicaConsistency(new LsnReplicaConsistency10());
            createTable(DualConnection.builder(connectionProvider, consistency).build());
            final Connection connection = DualConnection.builder(
                connectionProvider,
                consistency
            ).build();

            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo(bar) VALUES(?);");
            preparedStatement.setString(1, "test");

            final Throwable throwable = catchThrowable(preparedStatement::executeUpdate);
            final boolean readOnly = connection.isReadOnly();
            connection.close();

            assertThat(readOnly).isTrue();
            assertThat(throwable).hasMessage("ERROR: cannot execute INSERT in a read-only transaction");
        }
    }

    @Test
    public void shouldRunNextValOnMainDatabase() throws SQLException {
        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            final WaitingReplicaConsistency consistency = new WaitingReplicaConsistency(new LsnReplicaConsistency10());
            createSequence(DualConnection.builder(connectionProvider, consistency).build(), "test_sequence");
            final Connection connection = DualConnection.builder(connectionProvider, consistency).build();

            connection.prepareStatement("SELECT nextval('test_sequence');").executeQuery();
        }
    }

    @Test
    public void shluldNotFailWhenChangingTransactionIsolationLevel() throws SQLException {
        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            final Connection connection = DualConnection.builder(
                connectionProvider,
                CircularConsistency.permanentConsistency().build()
            ).build();

            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            connection.setAutoCommit(false);
            connection.prepareStatement("SELECT 1;").executeQuery();
            connection.prepareStatement("SELECT 1;").executeQuery();
            connection.commit();
        }
    }

    @SuppressWarnings({"ThrowableNotThrown"})
    @Test
    public void shouldImplementAllConnectionMethods() throws SQLException {
        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            final Connection connection = DualConnection.builder(
                connectionProvider,
                new LsnReplicaConsistency10()
            ).build();
            connection.createStatement();
            connection.prepareStatement(SIMPLE_QUERY);
            connection.prepareCall(SIMPLE_QUERY);
            connection.nativeSQL(SIMPLE_QUERY);
            connection.setAutoCommit(true);
            assertThat(connection.getAutoCommit()).isTrue();
            connection.setAutoCommit(false);
            assertThat(connection.getAutoCommit()).isFalse();
            connection.commit();
            connection.rollback();
            connection.isClosed();
            connection.getMetaData();
            connection.setReadOnly(true);
            assertThat(connection.isReadOnly()).isTrue();
            connection.setReadOnly(false);
            assertThat(connection.isReadOnly()).isFalse();
            connection.setCatalog("catalog");
            assertThat(connection.getCatalog()).isEqualTo("catalog");
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.getTransactionIsolation();
            connection.getWarnings();
            connection.clearWarnings();
            connection.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
            connection.prepareStatement(SIMPLE_QUERY, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
            connection.prepareCall(SIMPLE_QUERY, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
            connection.getTypeMap();
            connection.setTypeMap(Collections.emptyMap());
            connection.setHoldability(CLOSE_CURSORS_AT_COMMIT);
            connection.getHoldability();
            final Savepoint savepoint = connection.setSavepoint();
            connection.setSavepoint("savepoint");
            connection.rollback(savepoint);
            connection.releaseSavepoint(savepoint);
            connection.createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, CLOSE_CURSORS_AT_COMMIT);
            connection.prepareStatement(SIMPLE_QUERY, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, CLOSE_CURSORS_AT_COMMIT);
            connection.prepareCall(SIMPLE_QUERY, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY, CLOSE_CURSORS_AT_COMMIT);
            connection.prepareStatement(SIMPLE_QUERY, NO_GENERATED_KEYS);
            connection.prepareStatement(SIMPLE_QUERY, new int[]{0});
            connection.prepareStatement(SIMPLE_QUERY, new String[]{"abcd"});
            assertThatThrownBy(connection::createClob).isInstanceOf(SQLFeatureNotSupportedException.class);
            assertThatThrownBy(connection::createBlob).isInstanceOf(SQLFeatureNotSupportedException.class);
            assertThatThrownBy(connection::createNClob).isInstanceOf(SQLFeatureNotSupportedException.class);
            connection.createSQLXML();
            connection.isValid(30);
            connection.getClientInfo();
            connection.getClientInfo("ApplicationName");
            connection.createArrayOf("float8", new Double[]{21.22});
            assertThatThrownBy(() -> connection.createStruct("float8", null))
                .isInstanceOf(SQLFeatureNotSupportedException.class);
            connection.unwrap(PgConnection.class);
            connection.isWrapperFor(PgConnection.class);

            connection.setSchema("public");
            assertThat(connection.getSchema()).isEqualTo("public");
            connection.setClientInfo("ApplicationName", "app");
            final Properties properties = new Properties();
            properties.setProperty("ApplicationName", "app");
            connection.setClientInfo(properties);
            final int timeout = (int) Duration.ofSeconds(30).toMillis();
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), timeout);
            assertThat(connection.getNetworkTimeout()).isEqualTo(timeout);
            connection.abort(Executors.newSingleThreadExecutor());

            connection.close();
        }
    }

    private void createSequence(Connection connection, String sequenceName) throws SQLException {
        try (final Statement mainStatement = connection.createStatement()) {
            mainStatement.execute("CREATE SEQUENCE " + sequenceName + ";");
        }
    }

    private void createTable(Connection connection) throws SQLException {
        try (final Statement mainStatement = connection.createStatement()) {
            mainStatement.execute("CREATE TABLE foo (bar VARCHAR ( 255 ));");
        }
    }

    @Test
    public void benchmarkReplicaConsistencies() throws InterruptedException, ExecutionException, SQLException {
        final LsnReplicaConsistency10 lsnConsistency = new LsnReplicaConsistency10(Cache.cacheMonotonicValuesInMemory());
        final LsnReplicaConsistency9 lsnConsistency9 = new LsnReplicaConsistency9(Cache.cacheMonotonicValuesInMemory());
        final ReplicaConsistency pessimisticPropagationConsistency = new PessimisticPropagationConsistency.Builder().assumeMaxPropagation(
            Duration.ZERO).build();
        final SequenceReplicaConsistency manualLsn = new SequenceReplicaConsistency("manualLsn",
            Cache.cacheMonotonicValuesInMemory());
//        benchmarkReplicaConsistency(manualLsn);
//        benchmarkReplicaConsistency(lsnConsistency);
        benchmarkReplicaConsistency(lsnConsistency9);
//        benchmarkReplicaConsistency(pessimisticPropagationConsistency);
    }

    public void benchmarkReplicaConsistency(ReplicaConsistency consistency) throws SQLException, InterruptedException, ExecutionException {
        final int concurrencyLevel = 100;
        final int times = 5000;
        final ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel + 1);
        final ExecutorCompletionService<Duration> completionService = new ExecutorCompletionService<>(
            executor);

        try (PostgresConnectionProvider connectionProvider = new PostgresConnectionProvider()) {
            final Connection mainConnection = DualConnection.builder(
                connectionProvider,
                consistency
            ).build();
            createSequence(mainConnection, "manualLsn");
            createSequence(mainConnection, "sequence1");
            executor.submit(() -> databaseModification(mainConnection));
            Duration totalExecutionTime = Duration.ZERO;
            for (int i = 0; i < concurrencyLevel; i++) {

                completionService.submit(() -> {
                    final Connection replicaConnection = connectionProvider.getReplicaConnection();
                    final Duration duration = measureConsistencyChecks(consistency, replicaConnection, times);
                    replicaConnection.close();
                    return duration;
                });
            }
            for (int i = 0; i < concurrencyLevel; i++) {
                final Future<Duration> executionTimeFeature = completionService.take();
                totalExecutionTime = totalExecutionTime.plus(executionTimeFeature.get());
            }
            System.out.println(consistency + "   Total: " + totalExecutionTime + ". Mean: " + totalExecutionTime.dividedBy(
                times * concurrencyLevel).toMillis());
        } finally {
            executor.shutdown();
        }

    }

    private void databaseModification(Connection connection) {
        final Statement mainStatement;
        try {
            mainStatement = connection.createStatement();
            while (true) {
                try {
                    final boolean execute = mainStatement.execute("SELECT NEXTVAL('sequence1');");
                    assert execute;
                } catch (Exception e) {
                    //ignore
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    private Duration measureConsistencyChecks(ReplicaConsistency consistency, Connection connection, int count) {
        Duration total = Duration.ZERO;
        for (int i = 0; i < count; i++) {
            total = total.plus(measureConsistency(
                consistency,
                connection
            ));
        }
        return total;
    }

    private Duration measureConsistency(ReplicaConsistency consistency, Connection connection) {
        final Instant start = Instant.now();
        consistency.isConsistent(() -> connection);
        return Duration.between(start, Instant.now());
    }

}
