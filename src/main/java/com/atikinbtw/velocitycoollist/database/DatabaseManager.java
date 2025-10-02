package com.atikinbtw.velocitycoollist.database;

import com.atikinbtw.velocitycoollist.Config;
import com.atikinbtw.velocitycoollist.VelocityCoolList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private static DatabaseManager INSTANCE;
    private HikariDataSource dataSource;
    private final VelocityCoolList plugin;
    private DatabaseType databaseType;
    
    public DatabaseManager(VelocityCoolList plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }
    
    public static DatabaseManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("DatabaseManager has not been initialized");
        }
        return INSTANCE;
    }
    
    public void initialize() {
        // Получаем тип базы данных из конфигурации
        String dbTypeStr = Config.getInstance().getString("database.type");
        this.databaseType = DatabaseType.fromString(dbTypeStr != null ? dbTypeStr : "sqlite");
        
        VelocityCoolList.LOGGER.info("Инициализация базы данных: {}", databaseType.getName());
        
        HikariConfig config = new HikariConfig();
        
        switch (databaseType) {
            case SQLITE -> setupSQLite(config);
            case MYSQL -> setupMySQL(config);
        }
        
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        try {
            this.dataSource = new HikariDataSource(config);
            createTables();
            VelocityCoolList.LOGGER.info("База данных успешно инициализирована");
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при инициализации базы данных: ", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    private void setupSQLite(HikariConfig config) {
        Path dbPath = plugin.DATADIRECTORY.resolve("whitelist.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbPath.toString());
        config.setDriverClassName("org.sqlite.JDBC");
        
        // SQLite specific settings
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("temp_store", "memory");
        config.addDataSourceProperty("mmap_size", "268435456"); // 256MB
    }
    
    private void setupMySQL(HikariConfig config) {
        String host = Config.getInstance().getString("database.mysql.host");
        int port = Config.getInstance().getInt("database.mysql.port");
        String database = Config.getInstance().getString("database.mysql.database");
        String username = Config.getInstance().getString("database.mysql.username");
        String password = Config.getInstance().getString("database.mysql.password");
        
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
                host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // MySQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }
    
    private void createTables() throws SQLException {
        String createTableSQL;
        
        if (databaseType == DatabaseType.SQLITE) {
            createTableSQL = """
                CREATE TABLE IF NOT EXISTS whitelist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(255) NOT NULL UNIQUE COLLATE NOCASE,
                    added_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """;
        } else {
            createTableSQL = """
                CREATE TABLE IF NOT EXISTS whitelist (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) NOT NULL UNIQUE,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_username (username)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """;
        }
        
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }
    
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource is not initialized");
        }
        return dataSource.getConnection();
    }
    
    public CompletableFuture<Boolean> addPlayer(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR IGNORE INTO whitelist (username) VALUES (?)";
            if (databaseType == DatabaseType.MYSQL) {
                sql = "INSERT IGNORE INTO whitelist (username) VALUES (?)";
            }
            
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, username);
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
                
            } catch (SQLException e) {
                VelocityCoolList.LOGGER.error("Ошибка при добавлении игрока в whitelist: ", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> removePlayer(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql;
            if (databaseType == DatabaseType.SQLITE) {
                sql = "DELETE FROM whitelist WHERE username = ? COLLATE NOCASE";
            } else {
                sql = "DELETE FROM whitelist WHERE LOWER(username) = LOWER(?)";
            }
            
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, username);
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
                
            } catch (SQLException e) {
                VelocityCoolList.LOGGER.error("Ошибка при удалении игрока из whitelist: ", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> containsPlayer(String username) {
        return CompletableFuture.supplyAsync(() -> {
            String sql;
            if (databaseType == DatabaseType.SQLITE) {
                sql = "SELECT 1 FROM whitelist WHERE username = ? COLLATE NOCASE LIMIT 1";
            } else {
                sql = "SELECT 1 FROM whitelist WHERE LOWER(username) = LOWER(?) LIMIT 1";
            }
            
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.setString(1, username);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
                
            } catch (SQLException e) {
                VelocityCoolList.LOGGER.error("Ошибка при проверке игрока в whitelist: ", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<List<String>> getAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> players = new ArrayList<>();
            String sql = "SELECT username FROM whitelist ORDER BY username";
            
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                while (resultSet.next()) {
                    players.add(resultSet.getString("username"));
                }
                
            } catch (SQLException e) {
                VelocityCoolList.LOGGER.error("Ошибка при получении списка игроков: ", e);
            }
            
            return players;
        });
    }
    
    public CompletableFuture<Boolean> clearWhitelist() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM whitelist";
            
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                
                statement.executeUpdate();
                return true;
                
            } catch (SQLException e) {
                VelocityCoolList.LOGGER.error("Ошибка при очистке whitelist: ", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> isEmpty() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM whitelist LIMIT 1";
            
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                
                return !resultSet.next();
                
            } catch (SQLException e) {
                VelocityCoolList.LOGGER.error("Ошибка при проверке пустоты whitelist: ", e);
                return true;
            }
        });
    }
    
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            VelocityCoolList.LOGGER.info("База данных отключена");
        }
    }
}
