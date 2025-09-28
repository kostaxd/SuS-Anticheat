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
import org.bukkit.util.Vector;

public class KillAuraCheck extends Check {
    
    private double maxAttackAngle;
    private int minAttackDelay;
    
    public KillAuraCheck(SuSAntiCheat plugin) {
        super(plugin, "KillAura", "combat.killaura");
    }
    
    @Override
    public void loadConfig() {
        super.loadConfig();
        maxAttackAngle = plugin.getConfigManager().getDouble("detections.combat.killaura.max-attack-angle", 45.0);
        minAttackDelay = plugin.getConfigManager().getInt("detections.combat.killaura.min-attack-delay", 150);
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
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long lastAttackTime = playerData.getStatistic("lastAttackTime", Long.class, 0L);
        
        // Check attack delay
        if (lastAttackTime > 0) {
            long attackDelay = currentTime - lastAttackTime;
            if (attackDelay < minAttackDelay) {
                int fastAttacks = playerData.getStatistic("fastAttacks", Integer.class, 0);
                playerData.updateStatistic("fastAttacks", fastAttacks + 1);
                
                if (fastAttacks >= 5) {
                    flag(player, String.format("Rapid attacks detected (Delay: %dms, Count: %d)", 
                        attackDelay, fastAttacks));
                    playerData.updateStatistic("fastAttacks", 0);
                }
            } else {
                playerData.updateStatistic("fastAttacks", 0);
            }
        }
        
        // Check attack angle
        Location playerLoc = player.getLocation();
        Location victimLoc = victim.getLocation();
        
        // Calculate angle between player's view direction and direction to victim
        Vector playerDirection = playerLoc.getDirection().normalize();
        Vector toVictim = victimLoc.toVector().subtract(playerLoc.toVector()).normalize();
        
        double angle = Math.toDegrees(Math.acos(playerDirection.dot(toVictim)));
        
        if (angle > maxAttackAngle) {
            int angleViolations = playerData.getStatistic("angleViolations", Integer.class, 0);
            playerData.updateStatistic("angleViolations", angleViolations + 1);
            
            if (angleViolations >= 3) {
                flag(player, String.format("Suspicious attack angle (Angle: %.1f°, Max: %.1f°)", 
                    angle, maxAttackAngle));
                playerData.updateStatistic("angleViolations", 0);
            }
        } else {
            playerData.updateStatistic("angleViolations", 0);
        }
        
        // Check for multi-target attacks
        String lastVictimId = playerData.getStatistic("lastVictimId", String.class, "");
        String currentVictimId = victim.getUniqueId().toString();
        
        if (!lastVictimId.equals(currentVictimId)) {
            long lastTargetSwitch = playerData.getStatistic("lastTargetSwitch", Long.class, 0L);
            if (currentTime - lastTargetSwitch < 500) { // Target switched within 500ms
                int targetSwitches = playerData.getStatistic("targetSwitches", Integer.class, 0);
                playerData.updateStatistic("targetSwitches", targetSwitches + 1);
                
                if (targetSwitches >= 4) {
                    flag(player, String.format("Rapid target switching (Switches: %d)", targetSwitches));
                    playerData.updateStatistic("targetSwitches", 0);
                }
            } else {
                playerData.updateStatistic("targetSwitches", 0);
            }
            playerData.updateStatistic("lastTargetSwitch", currentTime);
        }
        
        // Update tracking data
        playerData.updateStatistic("lastAttackTime", currentTime);
        playerData.updateStatistic("lastVictimId", currentVictimId);
    }
    
    @Override
    protected boolean isHighPriority() {
        return true;
    }
}