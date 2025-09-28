package com.susanticheat.data.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ViolationRecord {
    
    private final int id;
    private final UUID playerUUID;
    private final String checkName;
    private final int violationLevel;
    private final long timestamp;
    private final double serverTPS;
    private final int playerPing;
    private final Map<String, Object> additionalData;
    
    public ViolationRecord(UUID playerUUID, String checkName, int violationLevel, 
                          double serverTPS, int playerPing) {
        this.id = -1; // Will be set by database
        this.playerUUID = playerUUID;
        this.checkName = checkName;
        this.violationLevel = violationLevel;
        this.timestamp = System.currentTimeMillis();
        this.serverTPS = serverTPS;
        this.playerPing = playerPing;
        this.additionalData = new HashMap<>();
    }
    
    private ViolationRecord(int id, UUID playerUUID, String checkName, int violationLevel,
                           long timestamp, double serverTPS, int playerPing, 
                           Map<String, Object> additionalData) {
        this.id = id;
        this.playerUUID = playerUUID;
        this.checkName = checkName;
        this.violationLevel = violationLevel;
        this.timestamp = timestamp;
        this.serverTPS = serverTPS;
        this.playerPing = playerPing;
        this.additionalData = additionalData != null ? additionalData : new HashMap<>();
    }
    
    public static ViolationRecord fromResultSet(ResultSet rs) throws SQLException {
        UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
        
        // Parse additional data JSON
        Map<String, Object> additionalData = new HashMap<>();
        String additionalDataJson = rs.getString("additional_data");
        if (additionalDataJson != null && !additionalDataJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            additionalData = gson.fromJson(additionalDataJson, type);
        }
        
        return new ViolationRecord(
            rs.getInt("id"),
            playerUUID,
            rs.getString("check_name"),
            rs.getInt("violation_level"),
            rs.getTimestamp("timestamp").getTime(),
            rs.getDouble("server_tps"),
            rs.getInt("player_ping"),
            additionalData
        );
    }
    
    public void addData(String key, Object value) {
        additionalData.put(key, value);
    }
    
    public String getAdditionalDataAsJson() {
        Gson gson = new Gson();
        return gson.toJson(additionalData);
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getCheckName() {
        return checkName;
    }
    
    public int getViolationLevel() {
        return violationLevel;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public double getServerTPS() {
        return serverTPS;
    }
    
    public int getPlayerPing() {
        return playerPing;
    }
    
    public Map<String, Object> getAdditionalData() {
        return new HashMap<>(additionalData);
    }
    
    @Override
    public String toString() {
        return String.format("ViolationRecord{player=%s, check=%s, level=%d, time=%d}", 
                           playerUUID, checkName, violationLevel, timestamp);
    }
}