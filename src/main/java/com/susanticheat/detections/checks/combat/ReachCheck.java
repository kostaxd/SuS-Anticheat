package com.susanticheat.detections.checks.combat;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.detections.Check;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ReachCheck extends Check {
    
    private double maxReach;
    
    public ReachCheck(SuSAntiCheat plugin) {
        super(plugin, "Reach", "combat.reach");
    }
    
    @Override
    public void loadConfig() {
        super.loadConfig();
        maxReach = plugin.getConfigManager().getDouble("detections.combat.reach.max-reach", 4.2);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        
        if (!shouldCheck(player)) {
            return;
        }
        
        Entity victim = event.getEntity();
        if (!(victim instanceof LivingEntity)) {
            return;
        }
        
        // Calculate distance between player and victim
        Location playerLoc = player.getLocation();
        Location victimLoc = victim.getLocation();
        
        // Account for player and victim bounding boxes
        double distance = playerLoc.distance(victimLoc);
        
        // Adjust for entity hitboxes (approximate)
        distance -= 0.6; // Player hitbox width
        distance -= getEntityHitboxRadius(victim);
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        if (distance > maxReach) {
            int reachViolations = playerData.getStatistic("reachViolations", Integer.class, 0);
            playerData.updateStatistic("reachViolations", reachViolations + 1);
            
            // Track maximum reach violation
            double maxReachViolation = playerData.getStatistic("maxReachViolation", Double.class, 0.0);
            if (distance > maxReachViolation) {
                playerData.updateStatistic("maxReachViolation", distance);
            }
            
            if (reachViolations >= 3) {
                flag(player, String.format("Reach violation (Distance: %.2f, Max: %.2f, Max violation: %.2f)", 
                    distance, maxReach, maxReachViolation));
                
                // Reset violations after flagging
                playerData.updateStatistic("reachViolations", 0);
            }
        } else {
            // Decay violations if legitimate hit
            int currentViolations = playerData.getStatistic("reachViolations", Integer.class, 0);
            if (currentViolations > 0) {
                playerData.updateStatistic("reachViolations", Math.max(0, currentViolations - 1));
            }
        }
        
        // Store last legitimate distance for analysis
        if (distance <= maxReach) {
            playerData.updateStatistic("lastLegitDistance", distance);
        }
    }
    
    private double getEntityHitboxRadius(Entity entity) {
        // Rough approximation of entity hitbox radii
        return switch (entity.getType()) {
            case PLAYER -> 0.3;
            case ZOMBIE, SKELETON, CREEPER -> 0.3;
            case SPIDER -> 0.7;
            case ENDERMAN -> 0.3;
            case SLIME -> 0.6;
            case GHAST -> 2.0;
            default -> 0.3;
        };
    }
    
    @Override
    protected boolean isHighPriority() {
        return true;
    }
}