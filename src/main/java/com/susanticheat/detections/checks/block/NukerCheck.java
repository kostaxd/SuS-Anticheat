package com.susanticheat.detections.checks.block;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.detections.Check;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NukerCheck extends Check {
    
    private int maxBlocksPerSecond;
    
    public NukerCheck(SuSAntiCheat plugin) {
        super(plugin, "Nuker", "block.nuker");
    }
    
    @Override
    public void loadConfig() {
        super.loadConfig();
        maxBlocksPerSecond = plugin.getConfigManager().getInt("detections.block.nuker.max-blocks-per-second", 15);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (!shouldCheck(player)) {
            return;
        }
        
        // Skip creative mode players
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get or create break times queue
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<Long> breakTimes = (ConcurrentLinkedQueue<Long>) 
            playerData.getStatistic("blockBreakTimes", ConcurrentLinkedQueue.class, new ConcurrentLinkedQueue<>());
        
        // Add current break time
        breakTimes.offer(currentTime);
        
        // Remove entries older than 1 second
        breakTimes.removeIf(time -> currentTime - time > 1000);
        
        // Update statistics
        playerData.updateStatistic("blockBreakTimes", breakTimes);
        
        // Check if too many blocks broken in the last second
        int blocksInLastSecond = breakTimes.size();
        
        if (blocksInLastSecond > maxBlocksPerSecond) {
            flag(player, String.format("Nuker detected (Blocks/sec: %d, Max: %d)", 
                blocksInLastSecond, maxBlocksPerSecond));
        }
        
        // Additional check for suspicious breaking patterns
        if (breakTimes.size() >= 5) {
            // Check if breaks are too evenly spaced (bot-like behavior)
            Long[] times = breakTimes.toArray(new Long[0]);
            boolean evenlySpaced = true;
            
            if (times.length >= 5) {
                long firstInterval = times[1] - times[0];
                for (int i = 2; i < Math.min(times.length, 10); i++) {
                    long interval = times[i] - times[i-1];
                    if (Math.abs(interval - firstInterval) > 50) { // 50ms tolerance
                        evenlySpaced = false;
                        break;
                    }
                }
                
                if (evenlySpaced && firstInterval < 200) { // Less than 200ms between breaks
                    flag(player, String.format("Automated breaking pattern (Interval: %dms)", firstInterval));
                }
            }
        }
    }
}