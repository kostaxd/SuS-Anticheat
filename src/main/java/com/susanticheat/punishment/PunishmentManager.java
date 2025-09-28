package com.susanticheat.punishment;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.utils.AlertManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class PunishmentManager {
    
    private final SuSAntiCheat plugin;
    
    public PunishmentManager(SuSAntiCheat plugin) {
        this.plugin = plugin;
    }
    
    public void executePunishment(Player player, String punishmentType, String reason) {
        if (!isEnabled(punishmentType)) {
            return;
        }
        
        switch (punishmentType.toLowerCase()) {
            case "warn" -> executeWarn(player, reason);
            case "kick" -> executeKick(player, reason);
            case "tempban" -> executeTempBan(player, reason);
            case "ban" -> executeBan(player, reason);
            default -> plugin.getLogger().warning("Unknown punishment type: " + punishmentType);
        }
    }
    
    private void executeWarn(Player player, String reason) {
        String message = getFormattedMessage("warn", reason);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        
        if (shouldAlertStaff("warn")) {
            AlertManager.sendStaffMessage(String.format("&e[SuS] &7Player &f%s &7has been warned for: &f%s", 
                player.getName(), reason));
        }
        
        if (shouldLog("warn")) {
            plugin.getLogManager().logPunishment(player, "WARN", reason, null);
        }
    }
    
    private void executeKick(Player player, String reason) {
        String message = getFormattedMessage("kick", reason);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', message));
        });
        
        if (shouldAlertStaff("kick")) {
            AlertManager.sendStaffMessage(String.format("&c[SuS] &7Player &f%s &7has been kicked for: &f%s", 
                player.getName(), reason));
        }
        
        if (shouldLog("kick")) {
            plugin.getLogManager().logPunishment(player, "KICK", reason, null);
        }
    }
    
    private void executeTempBan(Player player, String reason) {
        int duration = plugin.getConfigManager().getInt("punishments.tempban.duration", 3600);
        String message = getFormattedMessage("tempban", reason);
        
        Instant expiry = Instant.now().plus(duration, ChronoUnit.SECONDS);
        Date expiryDate = Date.from(expiry);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, expiryDate, "SuS AntiCheat");
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', message));
        });
        
        if (shouldAlertStaff("tempban")) {
            AlertManager.sendStaffMessage(String.format("&6[SuS] &7Player &f%s &7has been temporarily banned for: &f%s &7(Duration: %d seconds)", 
                player.getName(), reason, duration));
        }
        
        if (shouldLog("tempban")) {
            plugin.getLogManager().logPunishment(player, "TEMPBAN", reason, expiryDate);
        }
    }
    
    private void executeBan(Player player, String reason) {
        String message = getFormattedMessage("ban", reason);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, (Date) null, "SuS AntiCheat");
            player.kickPlayer(ChatColor.translateAlternateColorCodes('&', message));
        });
        
        if (shouldAlertStaff("ban")) {
            AlertManager.sendStaffMessage(String.format("&4[SuS] &7Player &f%s &7has been permanently banned for: &f%s", 
                player.getName(), reason));
        }
        
        if (shouldLog("ban")) {
            plugin.getLogManager().logPunishment(player, "BAN", reason, null);
        }
    }
    
    private String getFormattedMessage(String punishmentType, String reason) {
        String configPath = "punishments." + punishmentType + ".message";
        String message = plugin.getConfigManager().getString(configPath, "&c[SuS] You have been punished!");
        
        return message.replace("{reason}", reason)
                     .replace("{player}", "{player}"); // Can be expanded for more placeholders
    }
    
    private boolean isEnabled(String punishmentType) {
        return plugin.getConfigManager().getBoolean("punishments." + punishmentType + ".enabled", true);
    }
    
    private boolean shouldAlertStaff(String punishmentType) {
        return plugin.getConfigManager().getBoolean("punishments." + punishmentType + ".alert-staff", true);
    }
    
    private boolean shouldLog(String punishmentType) {
        return plugin.getConfigManager().getBoolean("punishments." + punishmentType + ".log", true);
    }
}