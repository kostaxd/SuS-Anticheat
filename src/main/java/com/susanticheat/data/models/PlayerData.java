package com.susanticheat.data.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    
    private final UUID uuid;
    private String username;
    private int totalViolations;
    private long lastSeen;
    private boolean banned;
    private String banReason;
    private Long banTimestamp;
    private final Map<String, Integer> violationCounts;
    private final Map<String, Long> lastViolationTime;
    private final Map<String, Object> statistics;
    
    // Transient data (not stored in database)
    private transient final Map<String, Long> lastCheckTime;
    private transient double suspicionLevel;
    
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.violationCounts = new ConcurrentHashMap<>();
        this.lastViolationTime = new ConcurrentHashMap<>();
        this.statistics = new ConcurrentHashMap<>();
        this.lastCheckTime = new ConcurrentHashMap<>();
        this.totalViolations = 0;
        this.lastSeen = System.currentTimeMillis();
        this.banned = false;
        this.suspicionLevel = 0.0;
    }
    
    public static PlayerData fromResultSet(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        PlayerData data = new PlayerData(uuid);
        
        data.username = rs.getString("username");
        data.totalViolations = rs.getInt("total_violations");
        data.lastSeen = rs.getTimestamp("last_seen").getTime();
        data.banned = rs.getBoolean("is_banned");
        data.banReason = rs.getString("ban_reason");
        
        if (rs.getTimestamp("ban_timestamp") != null) {
            data.banTimestamp = rs.getTimestamp("ban_timestamp").getTime();
        }
        
        // Parse statistics JSON
        String statisticsJson = rs.getString("statistics");
        if (statisticsJson != null && !statisticsJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> stats = gson.fromJson(statisticsJson, type);
            data.statistics.putAll(stats);
        }
        
        return data;
    }
    
    // Violation management
    public void addViolation(String checkName) {
        violationCounts.merge(checkName, 1, Integer::sum);
        lastViolationTime.put(checkName, System.currentTimeMillis());
        totalViolations++;
        
        // Update suspicion level
        updateSuspicionLevel();
    }
    
    public int getViolations(String checkName) {
        return violationCounts.getOrDefault(checkName, 0);
    }
    
    public void resetViolations(String checkName) {
        violationCounts.remove(checkName);
        lastViolationTime.remove(checkName);
    }
    
    public void decayViolations(String checkName, int decayAmount) {
        int current = getViolations(checkName);
        if (current > 0) {
            int newAmount = Math.max(0, current - decayAmount);
            if (newAmount == 0) {
                resetViolations(checkName);
            } else {
                violationCounts.put(checkName, newAmount);
            }
        }
    }
    
    // Time-based checks
    public boolean canCheck(String checkName, long cooldown) {
        long lastCheck = lastCheckTime.getOrDefault(checkName, 0L);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCheck >= cooldown) {
            lastCheckTime.put(checkName, currentTime);
            return true;
        }
        return false;
    }
    
    public long getTimeSinceLastViolation(String checkName) {
        Long lastTime = lastViolationTime.get(checkName);
        return lastTime != null ? System.currentTimeMillis() - lastTime : Long.MAX_VALUE;
    }
    
    // Statistics management
    public void updateStatistic(String key, Object value) {
        statistics.put(key, value);
    }
    
    public <T> T getStatistic(String key, Class<T> type, T defaultValue) {
        Object value = statistics.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return defaultValue;
    }
    
    // Suspicion level calculation
    private void updateSuspicionLevel() {
        double baseSuspicion = 0.0;
        
        // Base suspicion on total violations
        baseSuspicion += Math.min(totalViolations * 0.1, 5.0);
        
        // Add suspicion based on recent violations
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : lastViolationTime.entrySet()) {
            long timeSince = currentTime - entry.getValue();
            if (timeSince < 300000) { // 5 minutes
                baseSuspicion += 0.5;
            }
        }
        
        // Cap suspicion level
        this.suspicionLevel = Math.min(baseSuspicion, 10.0);
    }
    
    // JSON serialization for database storage
    public String getStatisticsAsJson() {
        Gson gson = new Gson();
        return gson.toJson(statistics);
    }
    
    // Getters and setters
    public UUID getUuid() {
        return uuid;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public int getTotalViolations() {
        return totalViolations;
    }
    
    public void setTotalViolations(int totalViolations) {
        this.totalViolations = totalViolations;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public boolean isBanned() {
        return banned;
    }
    
    public void setBanned(boolean banned) {
        this.banned = banned;
    }
    
    public String getBanReason() {
        return banReason;
    }
    
    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }
    
    public Long getBanTimestamp() {
        return banTimestamp;
    }
    
    public void setBanTimestamp(Long banTimestamp) {
        this.banTimestamp = banTimestamp;
    }
    
    public double getSuspicionLevel() {
        return suspicionLevel;
    }
    
    public Map<String, Integer> getViolationCounts() {
        return new HashMap<>(violationCounts);
    }
    
    public Map<String, Object> getStatistics() {
        return new HashMap<>(statistics);
    }
}