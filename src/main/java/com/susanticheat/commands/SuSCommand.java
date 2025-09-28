package com.susanticheat.commands;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.data.models.PlayerData;
import com.susanticheat.data.models.ViolationRecord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SuSCommand implements CommandExecutor {
    
    private final SuSAntiCheat plugin;
    
    public SuSCommand(SuSAntiCheat plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sus.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        switch (command.getName().toLowerCase()) {
            case "sus" -> handleMainCommand(sender, args);
            case "susreload" -> handleReload(sender);
            case "susinfo" -> handleInfo(sender, args);
            case "susban" -> handleBan(sender, args);
            case "suskick" -> handleKick(sender, args);
        }
        
        return true;
    }
    
    private void handleMainCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "help" -> sendHelpMessage(sender);
            case "reload" -> handleReload(sender);
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /sus info <player>");
                    return;
                }
                handleInfo(sender, new String[]{args[1]});
            }
            case "stats" -> handleStats(sender);
            case "version" -> handleVersion(sender);
            default -> sendHelpMessage(sender);
        }
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SuS AntiCheat Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/sus help - Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/sus reload - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/sus info <player> - Get player information");
        sender.sendMessage(ChatColor.YELLOW + "/sus stats - Show server statistics");
        sender.sendMessage(ChatColor.YELLOW + "/sus version - Show plugin version");
        sender.sendMessage(ChatColor.YELLOW + "/suskick <player> [reason] - Kick a player");
        sender.sendMessage(ChatColor.YELLOW + "/susban <player> [reason] - Ban a player");
    }
    
    private void handleReload(CommandSender sender) {
        try {
            plugin.reload();
            sender.sendMessage(ChatColor.GREEN + "SuS AntiCheat configuration reloaded successfully!");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reload configuration: " + e.getMessage());
        }
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /sus info <player>");
            return;
        }
        
        String playerName = args[0];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (playerData == null) {
            sender.sendMessage(ChatColor.RED + "No data found for player!");
            return;
        }
        
        // Send player info
        sender.sendMessage(ChatColor.GOLD + "=== Player Info: " + target.getName() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Total Violations: " + ChatColor.WHITE + playerData.getTotalViolations());
        sender.sendMessage(ChatColor.YELLOW + "Suspicion Level: " + ChatColor.WHITE + 
            String.format("%.1f/10.0", playerData.getSuspicionLevel()));
        sender.sendMessage(ChatColor.YELLOW + "Banned: " + ChatColor.WHITE + 
            (playerData.isBanned() ? "Yes" : "No"));
        
        // Show individual check violations
        sender.sendMessage(ChatColor.GOLD + "Individual Violations:");
        playerData.getViolationCounts().forEach((check, violations) -> {
            if (violations > 0) {
                sender.sendMessage(ChatColor.GRAY + "- " + check + ": " + violations);
            }
        });
        
        // Get recent violations
        plugin.getDatabaseManager().getRecentViolations(target.getUniqueId(), 5)
            .thenAccept(violations -> {
                if (!violations.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GOLD + "Recent Violations:");
                        for (ViolationRecord violation : violations) {
                            sender.sendMessage(ChatColor.GRAY + "- " + violation.getCheckName() + 
                                " (Level " + violation.getViolationLevel() + ")");
                        }
                    });
                }
            });
    }
    
    private void handleStats(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== SuS AntiCheat Statistics ===");
        sender.sendMessage(ChatColor.YELLOW + "Plugin Version: " + ChatColor.WHITE + 
            plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "Enabled Checks: " + ChatColor.WHITE + 
            plugin.getDetectionManager().getEnabledChecks().size());
        sender.sendMessage(ChatColor.YELLOW + "Server TPS: " + ChatColor.WHITE + 
            String.format("%.1f", plugin.getCurrentTPS()));
        sender.sendMessage(ChatColor.YELLOW + "Performance Mode: " + ChatColor.WHITE + 
            (plugin.isPerformanceMode() ? "Enabled" : "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Online Players: " + ChatColor.WHITE + 
            Bukkit.getOnlinePlayers().size());
    }
    
    private void handleVersion(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "SuS AntiCheat v" + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "Running on " + Bukkit.getVersion());
        sender.sendMessage(ChatColor.GRAY + "Java Version: " + System.getProperty("java.version"));
    }
    
    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /suskick <player> [reason]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) 
            : "Kicked by administrator";
        
        plugin.getPunishmentManager().executePunishment(target, "kick", reason);
        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been kicked!");
    }
    
    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /susban <player> [reason]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return;
        }
        
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) 
            : "Banned by administrator";
        
        plugin.getPunishmentManager().executePunishment(target, "ban", reason);
        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been banned!");
    }
}