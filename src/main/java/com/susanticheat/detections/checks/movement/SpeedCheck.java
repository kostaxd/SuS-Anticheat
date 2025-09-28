package com.susanticheat.detections.checks.movement;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.detections.Check;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

public class SpeedCheck extends Check {
    
    private double maxSpeedMultiplier;
    
    public SpeedCheck(SuSAntiCheat plugin) {
        super(plugin, "Speed", "movement.speed");
    }
    
    @Override
    public void loadConfig() {
        super.loadConfig();
        maxSpeedMultiplier = plugin.getConfigManager().getDouble("detections.movement.speed.max-speed-multiplier", 1.5);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!shouldCheck(player)) {
            return;
        }
        
        // Skip if player is in creative or spectator mode
        if (player.getGameMode() == GameMode.CREATIVE || 
            player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) {
            return;
        }
        
        // Calculate horizontal distance
        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        
        // Base walking speed
        double baseSpeed = 0.2; // Default walking speed
        
        // Account for speed effects
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int amplifier = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier();
            baseSpeed += baseSpeed * 0.2 * (amplifier + 1);
        }
        
        // Account for sprinting
        if (player.isSprinting()) {
            baseSpeed *= 1.3;
        }
        
        // Calculate maximum allowed speed
        double maxAllowedSpeed = baseSpeed * maxSpeedMultiplier;
        
        // Get player data for tracking
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        // Only check if player moved significantly
        if (horizontalDistance > maxAllowedSpeed * 1.1) {
            // Get recent speed violations
            int recentViolations = playerData.getStatistic("speedViolations", Integer.class, 0);
            long lastViolationTime = playerData.getStatistic("lastSpeedViolation", Long.class, 0L);
            long currentTime = System.currentTimeMillis();
            
            // Reset counter if enough time has passed
            if (currentTime - lastViolationTime > 5000) { // 5 seconds
                recentViolations = 0;
            }
            
            recentViolations++;
            playerData.updateStatistic("speedViolations", recentViolations);
            playerData.updateStatistic("lastSpeedViolation", currentTime);
            
            // Flag if multiple violations in short time
            if (recentViolations >= 3) {
                flag(player, String.format("Speed violation (Distance: %.2f, Max: %.2f, Violations: %d)", 
                    horizontalDistance, maxAllowedSpeed, recentViolations));
                
                // Reset counter after flagging
                playerData.updateStatistic("speedViolations", 0);
            }
        }
        
        // Store last location and time for future calculations
        playerData.updateStatistic("lastLocation", from);
        playerData.updateStatistic("lastMoveTime", System.currentTimeMillis());
    }
    
    @Override
    protected boolean isHighPriority() {
        return true;
    }
}