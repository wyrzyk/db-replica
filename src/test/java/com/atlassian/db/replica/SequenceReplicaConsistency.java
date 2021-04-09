package com.atlassian.db.replica;

import com.atlassian.db.replica.spi.Cache;
import com.atlassian.db.replica.spi.ReplicaConsistency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.function.Supplier;

public class SequenceReplicaConsistency implements ReplicaConsistency {
    private final String sequenceName;
    private final Cache<Long> lastWrite;


    public SequenceReplicaConsistency(
        String sequenceName,
        Cache<Long> lastWrite
    ) {
        this.sequenceName = sequenceName;
        this.lastWrite = lastWrite;
    }


    @Override
    public void write(Connection main) {
        try {
            lastWrite.put(bumpLsn(main));
        } catch (Exception e) {
            //TODO: log warning
            lastWrite.reset();
        }
    }

    @Override
    public boolean isConsistent(Supplier<Connection> replica) {
        Optional<Long> maybeLastWrite = lastWrite.get();
        if (!maybeLastWrite.isPresent()) {
            return false;
        }
        Long lastRefresh;
        try {
            lastRefresh = queryLsn(replica.get());
        } catch (Exception e) {
            //TODO: log warning
            return false;
        }
        return lastRefresh >= maybeLastWrite.get();
    }

    private long bumpLsn(Connection connection) throws Exception {
        try (
            PreparedStatement query = prepareBumpQuery(connection);
            ResultSet results = query.executeQuery()
        ) {
            results.next();
            long lsn = results.getLong("nextval");
            return lsn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long queryLsn(Connection connection) throws Exception {
        try (
            PreparedStatement query = prepareQuery(connection);
            ResultSet results = query.executeQuery()
        ) {
            results.next();
            long lsn = results.getLong("last_value");
            return lsn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PreparedStatement prepareQuery(Connection connection) throws Exception {
        return connection.prepareStatement(
            "SELECT last_value FROM " + sequenceName
        );
    }

    private PreparedStatement prepareBumpQuery(Connection connection) throws Exception {
        return connection.prepareStatement(
            "SELECT nextval('" + sequenceName + "');"
        );
    }
}
