package com.susanticheat.utils;

import com.susanticheat.core.SuSAntiCheat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {
    
    private static final ConcurrentHashMap<String, Long> alertCooldowns = new ConcurrentHashMap<>();
    
    public static void sendAlert(Player player, String checkName, int violations, String details) {
        SuSAntiCheat plugin = SuSAntiCheat.getInstance();
        
        if (!plugin.getConfigManager().getBoolean("alerts.enabled", true)) {
            return;
        }
        
        // Check cooldown
        String cooldownKey = player.getUniqueId() + ":" + checkName;
        long currentTime = System.currentTimeMillis();
        long cooldown = plugin.getConfigManager().getInt("alerts.cooldown", 5) * 1000L;
        
        Long lastAlert = alertCooldowns.get(cooldownKey);
        if (lastAlert != null && (currentTime - lastAlert) < cooldown) {
            return; // Still in cooldown
        }
        
        alertCooldowns.put(cooldownKey, currentTime);
        
        // Format alert message
        String format = plugin.getConfigManager().getString("alerts.format", 
            "&c[SuS] &f{player} &7failed &f{check} &7(VL: &f{violations}&7) &8[&7{ping}ms&8]");
        
        String alertMessage = format
            .replace("{player}", player.getName())
            .replace("{check}", checkName)
            .replace("{violations}", String.valueOf(violations))
            .replace("{ping}", String.valueOf(player.getPing()))
            .replace("{details}", details);
        
        alertMessage = ChatColor.translateAlternateColorCodes('&', alertMessage);
        
        // Send to staff with permission
        String permission = plugin.getConfigManager().getString("alerts.permission", "sus.admin");
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(alertMessage);
            }
        }
        
        // Send to console if enabled
        if (plugin.getConfigManager().getBoolean("alerts.console", true)) {
            plugin.getLogger().info(ChatColor.stripColor(alertMessage));
        }
        
        // Log violation
        plugin.getLogManager().logViolation(player.getName(), checkName, violations, details);
    }
    
    public static void sendStaffMessage(String message) {
        SuSAntiCheat plugin = SuSAntiCheat.getInstance();
        String permission = plugin.getConfigManager().getString("alerts.permission", "sus.admin");
        
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(formattedMessage);
            }
        }
        
        plugin.getLogger().info(ChatColor.stripColor(formattedMessage));
    }
}