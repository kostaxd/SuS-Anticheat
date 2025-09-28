package com.susanticheat.detections.checks.network;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.detections.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketSpamCheck extends Check {
    
    private int maxPacketsPerSecond;
    
    public PacketSpamCheck(SuSAntiCheat plugin) {
        super(plugin, "PacketSpam", "network.packet-spam");
    }
    
    @Override
    public void loadConfig() {
        super.loadConfig();
        maxPacketsPerSecond = plugin.getConfigManager().getInt("detections.network.packet-spam.max-packets-per-second", 100);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        trackPacket(event.getPlayer(), "movement");
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            trackPacket(event.getPlayer(), "interact");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        trackPacket(event.getPlayer(), "swap_hands");
    }
    
    private void trackPacket(Player player, String packetType) {
        if (!shouldCheck(player)) {
            return;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Get or create packet times queue
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<Long> packetTimes = (ConcurrentLinkedQueue<Long>) 
            playerData.getStatistic("packetTimes", ConcurrentLinkedQueue.class, new ConcurrentLinkedQueue<>());
        
        // Add current packet time
        packetTimes.offer(currentTime);
        
        // Remove entries older than 1 second
        packetTimes.removeIf(time -> currentTime - time > 1000);
        
        // Update statistics
        playerData.updateStatistic("packetTimes", packetTimes);
        
        // Check if too many packets sent in the last second
        int packetsInLastSecond = packetTimes.size();
        
        if (packetsInLastSecond > maxPacketsPerSecond) {
            flag(player, String.format("Packet spam detected (Packets/sec: %d, Max: %d, Type: %s)", 
                packetsInLastSecond, maxPacketsPerSecond, packetType));
        }
        
        // Track specific packet type counts
        String packetTypeKey = "packets_" + packetType;
        int typeCount = playerData.getStatistic(packetTypeKey, Integer.class, 0);
        playerData.updateStatistic(packetTypeKey, typeCount + 1);
    }
}