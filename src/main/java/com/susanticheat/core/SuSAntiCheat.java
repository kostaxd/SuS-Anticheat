package com.susanticheat.core;

import com.susanticheat.commands.SuSCommand;
import com.susanticheat.config.ConfigManager;
import com.susanticheat.data.DatabaseManager;
import com.susanticheat.detections.DetectionManager;
import com.susanticheat.logging.LogManager;
import com.susanticheat.punishment.PunishmentManager;
import com.susanticheat.utils.PlayerDataManager;
import com.susanticheat.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class SuSAntiCheat extends JavaPlugin {
    
    private static SuSAntiCheat instance;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DetectionManager detectionManager;
    private PunishmentManager punishmentManager;
    private LogManager logManager;
    private PlayerDataManager playerDataManager;
    
    private double currentTPS = 20.0;
    private boolean performanceMode = false;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers in correct order
        initializeManagers();
        
        // Register events and commands
        registerEvents();
        registerCommands();
        
        // Start performance monitoring
        startPerformanceMonitoring();
        
        // Check for updates if enabled
        if (configManager.getBoolean("general.update-check")) {
            new UpdateChecker(this).checkForUpdates();
        }
        
        getLogger().info("SuS AntiCheat v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Protecting server with " + detectionManager.getEnabledChecks().size() + " detection modules.");
    }
    
    @Override
    public void onDisable() {
        // Cleanup and save data
        if (playerDataManager != null) {
            playerDataManager.saveAllPlayerData();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("SuS AntiCheat has been disabled!");
    }
    
    private void initializeManagers() {
        try {
            // Configuration must be loaded first
            configManager = new ConfigManager(this);
            
            // Database manager
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            
            // Logging manager
            logManager = new LogManager(this);
            
            // Player data manager
            playerDataManager = new PlayerDataManager(this);
            
            // Punishment manager
            punishmentManager = new PunishmentManager(this);
            
            // Detection manager (loads all detection modules)
            detectionManager = new DetectionManager(this);
            detectionManager.initialize();
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize SuS AntiCheat: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    
    private void registerEvents() {
        detectionManager.registerEvents();
    }
    
    private void registerCommands() {
        getCommand("sus").setExecutor(new SuSCommand(this));
        getCommand("susreload").setExecutor(new SuSCommand(this));
        getCommand("susinfo").setExecutor(new SuSCommand(this));
        getCommand("susban").setExecutor(new SuSCommand(this));
        getCommand("suskick").setExecutor(new SuSCommand(this));
    }
    
    private void startPerformanceMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                currentTPS = getCurrentTPS();
                double minTPS = configManager.getDouble("general.min-tps");
                
                if (currentTPS < minTPS && !performanceMode) {
                    performanceMode = true;
                    logManager.log(Level.WARNING, "Performance mode enabled due to low TPS: " + currentTPS);
                    detectionManager.enablePerformanceMode();
                } else if (currentTPS >= minTPS && performanceMode) {
                    performanceMode = false;
                    logManager.log(Level.INFO, "Performance mode disabled, TPS recovered: " + currentTPS);
                    detectionManager.disablePerformanceMode();
                }
            }
        }.runTaskTimer(this, 20L, 100L); // Check every 5 seconds
    }
    
    private double getCurrentTPS() {
        try {
            return Bukkit.getTPS()[0];
        } catch (Exception e) {
            return 20.0; // Fallback
        }
    }
    
    public void reload() {
        try {
            configManager.reloadConfig();
            detectionManager.reloadChecks();
            getLogger().info("SuS AntiCheat configuration reloaded successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to reload configuration: " + e.getMessage());
        }
    }
    
    // Getters for managers
    public static SuSAntiCheat getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public DetectionManager getDetectionManager() {
        return detectionManager;
    }
    
    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }
    
    public LogManager getLogManager() {
        return logManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public double getCurrentTPS() {
        return currentTPS;
    }
    
    public boolean isPerformanceMode() {
        return performanceMode;
    }
}