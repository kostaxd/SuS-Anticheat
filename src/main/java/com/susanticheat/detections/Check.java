package com.susanticheat.detections;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.data.models.ViolationRecord;
import com.susanticheat.utils.AlertManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;

public abstract class Check implements Listener {
    
    protected final SuSAntiCheat plugin;
    protected final String name;
    protected final String configPath;
    
    protected boolean enabled;
    protected int maxViolations;
    protected int violationDecay;
    protected String punishment;
    protected double sensitivity;
    protected boolean performanceMode = false;
    
    public Check(SuSAntiCheat plugin, String name, String configPath) {
        this.plugin = plugin;
        this.name = name;
        this.configPath = configPath;
        loadConfig();
    }
    
    public void loadConfig() {
        enabled = plugin.getConfigManager().isCheckEnabled(configPath);
        maxViolations = plugin.getConfigManager().getMaxViolations(configPath);
        violationDecay = plugin.getConfigManager().getViolationDecay(configPath);
        punishment = plugin.getConfigManager().getPunishment(configPath);
        sensitivity = plugin.getConfigManager().getSensitivity(configPath);
    }
    
    public void reloadConfig() {
        loadConfig();
    }
    
    protected boolean shouldCheck(Player player) {
        // Don't check players with bypass permission
        if (player.hasPermission("sus.bypass")) {
            return false;
        }
        
        // Don't check if disabled
        if (!enabled) {
            return false;
        }
        
        // Performance mode checks
        if (performanceMode && !isHighPriority()) {
            return false;
        }
        
        return true;
    }
    
    protected void flag(Player player, String details) {
        flag(player, details, 1);
    }
    
    protected void flag(Player player, String details, int violationIncrease) {
        if (!shouldCheck(player)) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        
        if (playerData == null) {
            return;
        }
        
        // Add violation
        for (int i = 0; i < violationIncrease; i++) {
            playerData.addViolation(name);
        }
        
        int violations = playerData.getViolations(name);
        
        // Create violation record
        ViolationRecord record = new ViolationRecord(
            playerUUID,
            name,
            violations,
            plugin.getCurrentTPS(),
            getPlayerPing(player)
        );
        record.addData("details", details);
        record.addData("location", player.getLocation().toString());
        
        // Log violation to database
        plugin.getDatabaseManager().logViolation(record);
        
        // Send alert to staff
        AlertManager.sendAlert(player, name, violations, details);
        
        // Execute punishment if threshold reached
        if (violations >= maxViolations) {
            plugin.getPunishmentManager().executePunishment(player, punishment, 
                "Cheating detected: " + name + " (" + violations + " violations)");
        }
        
        // Log to console if debug enabled
        if (plugin.getConfigManager().getBoolean("general.debug")) {
            plugin.getLogger().info(String.format("[DEBUG] %s failed %s (VL: %d) - %s", 
                player.getName(), name, violations, details));
        }
    }
    
    private int getPlayerPing(Player player) {
        try {
            return player.getPing();
        } catch (Exception e) {
            return -1;
        }
    }
    
    protected boolean isHighPriority() {
        // Override in subclasses to mark high-priority checks
        return false;
    }
    
    public void enablePerformanceMode() {
        this.performanceMode = true;
    }
    
    public void disablePerformanceMode() {
        this.performanceMode = false;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getConfigPath() {
        return configPath;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getMaxViolations() {
        return maxViolations;
    }
    
    public String getPunishment() {
        return punishment;
    }
}