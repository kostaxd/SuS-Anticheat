package com.susanticheat.logging;

import com.susanticheat.core.SuSAntiCheat;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class LogManager {
    
    private final SuSAntiCheat plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    public LogManager(SuSAntiCheat plugin) {
        this.plugin = plugin;
        
        // Create logs directory
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
    }
    
    public void log(Level level, String message) {
        // Log to console
        plugin.getLogger().log(level, message);
        
        // Log to file if enabled
        if (plugin.getConfigManager().getBoolean("logging.file", true)) {
            logToFile(level, message);
        }
    }
    
    public void logViolation(String playerName, String checkName, int violations, String details) {
        String message = String.format("[VIOLATION] %s failed %s (VL: %d) - %s", 
                                     playerName, checkName, violations, details);
        log(Level.WARNING, message);
    }
    
    public void logPunishment(Player player, String punishmentType, String reason, Date expiry) {
        String expiryStr = expiry != null ? " (Expires: " + dateFormat.format(expiry) + ")" : "";
        String message = String.format("[PUNISHMENT] %s - %s: %s%s", 
                                     player.getName(), punishmentType, reason, expiryStr);
        log(Level.INFO, message);
        
        // Log to database if enabled
        if (plugin.getConfigManager().getBoolean("logging.database", true)) {
            // Database logging would be implemented here
        }
    }
    
    private void logToFile(Level level, String message) {
        CompletableFuture.runAsync(() -> {
            try {
                String filename = plugin.getConfigManager().getString("logging.filename", "logs/sus-%date%.log");
                filename = filename.replace("%date%", fileNameFormat.format(new Date()));
                
                File logFile = new File(plugin.getDataFolder(), filename);
                
                // Create parent directories if they don't exist
                if (!logFile.getParentFile().exists()) {
                    logFile.getParentFile().mkdirs();
                }
                
                try (FileWriter writer = new FileWriter(logFile, true)) {
                    String timestamp = dateFormat.format(new Date());
                    writer.write(String.format("[%s] [%s] %s%n", timestamp, level.getName(), message));
                }
                
                // Check file size and rotate if necessary
                rotateLogFileIfNeeded(logFile);
                
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
            }
        });
    }
    
    private void rotateLogFileIfNeeded(File logFile) {
        long maxSize = plugin.getConfigManager().getInt("logging.max-size", 50) * 1024 * 1024; // Convert MB to bytes
        
        if (logFile.length() > maxSize) {
            // Rotate the log file
            int maxFiles = plugin.getConfigManager().getInt("logging.max-files", 10);
            
            for (int i = maxFiles - 1; i > 0; i--) {
                File oldFile = new File(logFile.getParentFile(), logFile.getName() + "." + i);
                File newFile = new File(logFile.getParentFile(), logFile.getName() + "." + (i + 1));
                
                if (oldFile.exists()) {
                    if (i == maxFiles - 1) {
                        oldFile.delete(); // Delete the oldest file
                    } else {
                        oldFile.renameTo(newFile);
                    }
                }
            }
            
            // Move current file to .1
            File rotatedFile = new File(logFile.getParentFile(), logFile.getName() + ".1");
            logFile.renameTo(rotatedFile);
        }
    }
}