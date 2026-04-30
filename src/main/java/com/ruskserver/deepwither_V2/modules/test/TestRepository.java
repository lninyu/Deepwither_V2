package com.ruskserver.deepwither_V2.modules.test;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ruskserver.deepwither_V2.core.database.DatabaseManager;
import com.ruskserver.deepwither_V2.core.di.annotations.Ignore;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.repository.CachedRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Ignore
@Repository
public class TestRepository extends CachedRepository<String, String> implements Startable {

    private final Logger logger;

    @Inject
    public TestRepository(DatabaseManager db) {
        // JavaPlugin is not injected here directly, but we could if we needed it.
        // For now, we just use standard java logger or Bukkit logger.
        super(db);
        this.logger = Logger.getLogger("TestRepository");
    }

    @Override
    protected Cache<String, String> buildCache() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
    }

    @Override
    public void start() {
        // プラグイン起動時にテーブルを作成
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS test_data (" +
                             "key_name VARCHAR(255) PRIMARY KEY, " +
                             "value_data TEXT)")) {
            stmt.execute();
            logger.info("Checked/Created test_data table in H2.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to create test_data table", e);
        }
    }

    @Override
    protected Optional<String> loadFromDb(String key) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT value_data FROM test_data WHERE key_name = ?")) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("value_data"));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load data from DB for key: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    protected void saveToDb(String key, String value) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "MERGE INTO test_data (key_name, value_data) KEY(key_name) VALUES (?, ?)")) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to save data to DB for key: " + key, e);
        }
    }
}
