package com.susanticheat.detections.checks.movement;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.detections.Check;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class FlyCheck extends Check {
    
    public FlyCheck(SuSAntiCheat plugin) {
        super(plugin, "Fly", "movement.fly");
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
        
        // Skip if player is allowed to fly
        if (player.getAllowFlight()) {
            return;
        }
        
        // Get player data
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        Vector velocity = player.getVelocity();
        double yVelocity = velocity.getY();
        
        // Check for suspicious upward movement
        if (yVelocity > 0.5 && !isOnGround(player) && !hasNearbyBlocks(player)) {
            // Check if player has been airborne for too long
            long airTime = playerData.getStatistic("airTime", Long.class, 0L);
            if (airTime > 3000) { // 3 seconds airborne
                flag(player, String.format("Suspicious flight (Y-Vel: %.2f, Airborne: %dms)", 
                    yVelocity, airTime));
            }
        }
        
        // Update air time
        if (isOnGround(player)) {
            playerData.updateStatistic("airTime", 0L);
        } else {
            long lastUpdate = playerData.getStatistic("lastAirTimeUpdate", Long.class, 
                System.currentTimeMillis());
            long currentTime = System.currentTimeMillis();
            long currentAirTime = playerData.getStatistic("airTime", Long.class, 0L);
            playerData.updateStatistic("airTime", currentAirTime + (currentTime - lastUpdate));
            playerData.updateStatistic("lastAirTimeUpdate", currentTime);
        }
        
        // Check for hover detection
        if (Math.abs(yVelocity) < 0.1 && !isOnGround(player) && !hasNearbyBlocks(player)) {
            int hoverTicks = playerData.getStatistic("hoverTicks", Integer.class, 0);
            playerData.updateStatistic("hoverTicks", hoverTicks + 1);
            
            if (hoverTicks > 20) { // 1 second of hovering
                flag(player, String.format("Hovering detected (Ticks: %d)", hoverTicks));
            }
        } else {
            playerData.updateStatistic("hoverTicks", 0);
        }
    }
    
    private boolean isOnGround(Player player) {
        return player.isOnGround() || 
               player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
    }
    
    private boolean hasNearbyBlocks(Player player) {
        // Check for blocks within 2 blocks horizontally and 1 block vertically
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (player.getLocation().clone().add(x, y, z).getBlock().getType() != Material.AIR) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    protected boolean isHighPriority() {
        return true; // Fly is considered high priority
    }
}