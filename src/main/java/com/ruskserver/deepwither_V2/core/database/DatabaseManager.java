package com.ruskserver.deepwither_V2.core.database;

import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DatabaseManager implements Startable, Stoppable {

    private final JavaPlugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;

    @Inject
    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void start() {
        logger.info("Initializing Database Connection Pool...");

        File dataFolder = new File(plugin.getDataFolder(), "database");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // H2 Database file path (without .mv.db extension)
        File dbFile = new File(dataFolder, "data");
        String jdbcUrl = "jdbc:h2:" + dbFile.getAbsolutePath() + ";MODE=MySQL;AUTO_SERVER=TRUE";

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.h2.Driver");
        config.setUsername("sa");
        config.setPassword("");
        
        // HikariCP Settings optimized for Minecraft plugins
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setMaxLifetime(1800000); // 30 minutes
        config.setConnectionTimeout(10000); // 10 seconds
        config.setPoolName("Deepwither-H2-Pool");

        try {
            dataSource = new HikariDataSource(config);
            
            // Test connection
            try (Connection conn = dataSource.getConnection()) {
                logger.info("Successfully connected to H2 Database!");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize Database Manager!", e);
        }
    }

    @Override
    public void stop() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing Database Connection Pool...");
            dataSource.close();
        }
    }

    /**
     * Gets a connection from the HikariCP connection pool.
     * @return java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized!");
        }
        return dataSource.getConnection();
    }
}
