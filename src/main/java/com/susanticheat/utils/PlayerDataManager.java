package com.susanticheat.utils;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerDataManager implements Listener {
    
    private final SuSAntiCheat plugin;
    private final ConcurrentMap<UUID, PlayerData> playerDataCache;
    
    public PlayerDataManager(SuSAntiCheat plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        
        // Register events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start violation decay task
        startViolationDecayTask();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerData(player.getUniqueId(), player.getName());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        savePlayerData(player.getUniqueId());
        
        // Remove from cache after a delay to handle quick reconnects
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            playerDataCache.remove(player.getUniqueId());
        }, 1200L); // 60 seconds
    }
    
    private void loadPlayerData(UUID playerUUID, String playerName) {
        plugin.getDatabaseManager().getPlayerData(playerUUID).thenAccept(data -> {
            if (data != null) {
                data.setUsername(playerName);
                data.setLastSeen(System.currentTimeMillis());
                playerDataCache.put(playerUUID, data);
            }
        });
    }
    
    public void savePlayerData(UUID playerUUID) {
        PlayerData data = playerDataCache.get(playerUUID);
        if (data != null) {
            plugin.getDatabaseManager().savePlayerData(data);
        }
    }
    
    public void saveAllPlayerData() {
        for (PlayerData data : playerDataCache.values()) {
            plugin.getDatabaseManager().savePlayerData(data);
        }
    }
    
    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataCache.get(playerUUID);
    }
    
    private void startViolationDecayTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            
            for (PlayerData playerData : playerDataCache.values()) {
                // Decay violations for each check type
                for (String checkName : playerData.getViolationCounts().keySet()) {
                    long timeSinceLastViolation = playerData.getTimeSinceLastViolation(checkName);
                    int decayTime = plugin.getConfigManager().getViolationDecay(checkName) * 1000; // Convert to ms
                    
                    if (timeSinceLastViolation > decayTime) {
                        playerData.decayViolations(checkName, 1);
                    }
                }
            }
        }, 200L, 200L); // Run every 10 seconds
    }
}