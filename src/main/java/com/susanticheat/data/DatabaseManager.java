package com.susanticheat.data;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.data.models.ViolationRecord;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    
    private final SuSAntiCheat plugin;
    private Connection connection;
    private final String databaseType;
    
    public DatabaseManager(SuSAntiCheat plugin) {
        this.plugin = plugin;
        this.databaseType = plugin.getConfigManager().getString("database.type", "sqlite");
    }
    
    public void initialize() throws SQLException {
        createConnection();
        createTables();
    }
    
    private void createConnection() throws SQLException {
        if (databaseType.equalsIgnoreCase("sqlite")) {
            File databaseFile = new File(plugin.getDataFolder(), 
                plugin.getConfigManager().getString("database.sqlite-file", "sus_data.db"));
            
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }
            
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        } else if (databaseType.equalsIgnoreCase("mysql")) {
            // MySQL connection implementation
            String host = plugin.getConfigManager().getString("database.mysql.host");
            int port = plugin.getConfigManager().getInt("database.mysql.port");
            String database = plugin.getConfigManager().getString("database.mysql.database");
            String username = plugin.getConfigManager().getString("database.mysql.username");
            String password = plugin.getConfigManager().getString("database.mysql.password");
            
            String url = String.format("jdbc:mysql://%s:%d/%s", host, port, database);
            connection = DriverManager.getConnection(url, username, password);
        }
    }
    
    private void createTables() throws SQLException {
        // Player data table
        String playerDataTable = """
            CREATE TABLE IF NOT EXISTS player_data (
                uuid VARCHAR(36) PRIMARY KEY,
                username VARCHAR(16) NOT NULL,
                total_violations INTEGER DEFAULT 0,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_banned BOOLEAN DEFAULT FALSE,
                ban_reason TEXT,
                ban_timestamp TIMESTAMP,
                statistics TEXT
            )
        """;
        
        // Violations table
        String violationsTable = """
            CREATE TABLE IF NOT EXISTS violations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36) NOT NULL,
                check_name VARCHAR(50) NOT NULL,
                violation_level INTEGER NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                server_tps REAL,
                player_ping INTEGER,
                additional_data TEXT,
                FOREIGN KEY (player_uuid) REFERENCES player_data(uuid)
            )
        """;
        
        // Punishments table
        String punishmentsTable = """
            CREATE TABLE IF NOT EXISTS punishments (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36) NOT NULL,
                punishment_type VARCHAR(20) NOT NULL,
                reason TEXT,
                admin_uuid VARCHAR(36),
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                duration INTEGER DEFAULT 0,
                active BOOLEAN DEFAULT TRUE,
                FOREIGN KEY (player_uuid) REFERENCES player_data(uuid)
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(playerDataTable);
            stmt.execute(violationsTable);
            stmt.execute(punishmentsTable);
        }
    }
    
    // Async database operations to prevent blocking the main thread
    public CompletableFuture<PlayerData> getPlayerData(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE uuid = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return PlayerData.fromResultSet(rs);
                } else {
                    // Create new player data
                    return createPlayerData(playerUUID);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
                return null;
            }
        });
    }
    
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_data 
                (uuid, username, total_violations, last_seen, is_banned, ban_reason, ban_timestamp, statistics) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, playerData.getUsername());
                stmt.setInt(3, playerData.getTotalViolations());
                stmt.setTimestamp(4, new Timestamp(playerData.getLastSeen()));
                stmt.setBoolean(5, playerData.isBanned());
                stmt.setString(6, playerData.getBanReason());
                stmt.setTimestamp(7, playerData.getBanTimestamp() != null ? 
                    new Timestamp(playerData.getBanTimestamp()) : null);
                stmt.setString(8, playerData.getStatisticsAsJson());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Void> logViolation(ViolationRecord violation) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO violations 
                (player_uuid, check_name, violation_level, timestamp, server_tps, player_ping, additional_data) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, violation.getPlayerUUID().toString());
                stmt.setString(2, violation.getCheckName());
                stmt.setInt(3, violation.getViolationLevel());
                stmt.setTimestamp(4, new Timestamp(violation.getTimestamp()));
                stmt.setDouble(5, violation.getServerTPS());
                stmt.setInt(6, violation.getPlayerPing());
                stmt.setString(7, violation.getAdditionalDataAsJson());
                
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<List<ViolationRecord>> getRecentViolations(UUID playerUUID, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT * FROM violations 
                WHERE player_uuid = ? 
                ORDER BY timestamp DESC 
                LIMIT ?
            """;
            
            List<ViolationRecord> violations = new ArrayList<>();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setInt(2, limit);
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    violations.add(ViolationRecord.fromResultSet(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Database error: " + e.getMessage());
            }
            
            return violations;
        });
    }
    
    private PlayerData createPlayerData(UUID playerUUID) {
        PlayerData playerData = new PlayerData(playerUUID);
        savePlayerData(playerData);
        return playerData;
    }
    
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}