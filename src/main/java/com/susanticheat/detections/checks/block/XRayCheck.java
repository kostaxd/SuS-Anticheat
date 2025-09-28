package com.susanticheat.detections.checks.block;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.detections.Check;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumSet;
import java.util.Set;

public class XRayCheck extends Check {
    
    private double suspiciousOreRatio;
    private int checkDepth;
    
    private static final Set<Material> VALUABLE_ORES = EnumSet.of(
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE
    );
    
    public XRayCheck(SuSAntiCheat plugin) {
        super(plugin, "XRay", "block.xray");
    }
    
    @Override
    public void loadConfig() {
        super.loadConfig();
        suspiciousOreRatio = plugin.getConfigManager().getDouble("detections.block.xray.suspicious-ore-ratio", 0.15);
        checkDepth = plugin.getConfigManager().getInt("detections.block.xray.check-depth", 32);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (!shouldCheck(player)) {
            return;
        }
        
        Material blockType = event.getBlock().getType();
        
        // Only track blocks broken at mining depth
        if (event.getBlock().getY() > checkDepth) {
            return;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        // Update block break statistics
        int totalBlocks = playerData.getStatistic("totalBlocksBroken", Integer.class, 0);
        int oreBlocks = playerData.getStatistic("oreBlocksBroken", Integer.class, 0);
        
        totalBlocks++;
        
        if (VALUABLE_ORES.contains(blockType)) {
            oreBlocks++;
            
            // Track specific ore types
            String oreKey = blockType.name() + "_broken";
            int specificOreCount = playerData.getStatistic(oreKey, Integer.class, 0);
            playerData.updateStatistic(oreKey, specificOreCount + 1);
        }
        
        playerData.updateStatistic("totalBlocksBroken", totalBlocks);
        playerData.updateStatistic("oreBlocksBroken", oreBlocks);
        
        // Only check after a reasonable sample size
        if (totalBlocks >= 50) {
            double oreRatio = (double) oreBlocks / totalBlocks;
            
            if (oreRatio > suspiciousOreRatio) {
                // Additional checks to reduce false positives
                
                // Check if player is branch mining (more legitimate)
                boolean branchMining = checkBranchMiningPattern(playerData);
                
                if (!branchMining) {
                    flag(player, String.format("Suspicious ore ratio (Ratio: %.3f, Threshold: %.3f, Ores: %d/%d)", 
                        oreRatio, suspiciousOreRatio, oreBlocks, totalBlocks));
                    
                    // Reset statistics after flagging to avoid repeated flags
                    playerData.updateStatistic("totalBlocksBroken", 0);
                    playerData.updateStatistic("oreBlocksBroken", 0);
                }
            }
        }
        
        // Track mining patterns
        updateMiningPattern(playerData, event.getBlock().getLocation());
    }
    
    private boolean checkBranchMiningPattern(PlayerData playerData) {
        // Simple heuristic: if player breaks a lot of stone/deepslate, they're probably branch mining
        int stoneBlocks = playerData.getStatistic("STONE_broken", Integer.class, 0) +
                         playerData.getStatistic("DEEPSLATE_broken", Integer.class, 0);
        int totalBlocks = playerData.getStatistic("totalBlocksBroken", Integer.class, 1);
        
        double stoneRatio = (double) stoneBlocks / totalBlocks;
        return stoneRatio > 0.6; // If 60% of blocks are stone/deepslate, likely branch mining
    }
    
    private void updateMiningPattern(PlayerData playerData, org.bukkit.Location location) {
        // Store recent mining locations for pattern analysis
        // This could be expanded for more sophisticated pattern detection
        playerData.updateStatistic("lastMiningLocation", location.toString());
        playerData.updateStatistic("lastMiningTime", System.currentTimeMillis());
    }
}