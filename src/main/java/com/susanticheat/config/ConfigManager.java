package com.susanticheat.config;

import com.susanticheat.core.SuSAntiCheat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    
    private final SuSAntiCheat plugin;
    private FileConfiguration config;
    private File configFile;
    
    public ConfigManager(SuSAntiCheat plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        
        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Check for config version and update if needed
        checkConfigVersion();
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config: " + e.getMessage());
        }
    }
    
    private void checkConfigVersion() {
        // Future implementation for config version checking and migration
        // This ensures compatibility when updating the plugin
    }
    
    // Configuration getters with default values
    public String getString(String path) {
        return config.getString(path, "");
    }
    
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    public int getInt(String path) {
        return config.getInt(path, 0);
    }
    
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    public double getDouble(String path) {
        return config.getDouble(path, 0.0);
    }
    
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path, false);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    public void set(String path, Object value) {
        config.set(path, value);
    }
    
    // Specific configuration methods for common patterns
    public boolean isCheckEnabled(String checkName) {
        return getBoolean("detections." + checkName + ".enabled");
    }
    
    public int getMaxViolations(String checkName) {
        return getInt("detections." + checkName + ".max-violations", 5);
    }
    
    public int getViolationDecay(String checkName) {
        return getInt("detections." + checkName + ".violation-decay", 30);
    }
    
    public String getPunishment(String checkName) {
        return getString("detections." + checkName + ".punishment", "warn");
    }
    
    public double getSensitivity(String checkName) {
        return getDouble("detections." + checkName + ".sensitivity", 1.0);
    }
}