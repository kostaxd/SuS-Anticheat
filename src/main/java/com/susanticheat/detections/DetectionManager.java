package com.susanticheat.detections;

import com.susanticheat.core.SuSAntiCheat;
import com.susanticheat.detections.checks.block.NukerCheck;
import com.susanticheat.detections.checks.block.XRayCheck;
import com.susanticheat.detections.checks.combat.KillAuraCheck;
import com.susanticheat.detections.checks.combat.ReachCheck;
import com.susanticheat.detections.checks.movement.FlyCheck;
import com.susanticheat.detections.checks.movement.SpeedCheck;
import com.susanticheat.detections.checks.network.PacketSpamCheck;
import org.bukkit.Bukkit;

import java.util.*;

public class DetectionManager {
    
    private final SuSAntiCheat plugin;
    private final Map<String, Check> registeredChecks;
    private final Set<Check> enabledChecks;
    private boolean performanceMode = false;
    
    public DetectionManager(SuSAntiCheat plugin) {
        this.plugin = plugin;
        this.registeredChecks = new HashMap<>();
        this.enabledChecks = new HashSet<>();
    }
    
    public void initialize() {
        // Register all detection checks
        registerCheck(new FlyCheck(plugin));
        registerCheck(new SpeedCheck(plugin));
        registerCheck(new KillAuraCheck(plugin));
        registerCheck(new ReachCheck(plugin));
        registerCheck(new NukerCheck(plugin));
        registerCheck(new XRayCheck(plugin));
        registerCheck(new PacketSpamCheck(plugin));
        
        // Load enabled checks from configuration
        loadEnabledChecks();
        
        plugin.getLogger().info("Loaded " + enabledChecks.size() + " detection modules.");
    }
    
    private void registerCheck(Check check) {
        registeredChecks.put(check.getName().toLowerCase(), check);
    }
    
    private void loadEnabledChecks() {
        enabledChecks.clear();
        
        for (Check check : registeredChecks.values()) {
            if (plugin.getConfigManager().isCheckEnabled(check.getConfigPath())) {
                enabledChecks.add(check);
                plugin.getLogger().info("Enabled detection: " + check.getName());
            }
        }
    }
    
    public void registerEvents() {
        for (Check check : enabledChecks) {
            Bukkit.getPluginManager().registerEvents(check, plugin);
        }
    }
    
    public void reloadChecks() {
        // Reload configuration for all checks
        for (Check check : registeredChecks.values()) {
            check.reloadConfig();
        }
        
        // Reload enabled checks
        loadEnabledChecks();
    }
    
    public void enablePerformanceMode() {
        performanceMode = true;
        for (Check check : enabledChecks) {
            check.enablePerformanceMode();
        }
    }
    
    public void disablePerformanceMode() {
        performanceMode = false;
        for (Check check : enabledChecks) {
            check.disablePerformanceMode();
        }
    }
    
    // Getters
    public Set<Check> getEnabledChecks() {
        return new HashSet<>(enabledChecks);
    }
    
    public Check getCheck(String name) {
        return registeredChecks.get(name.toLowerCase());
    }
    
    public boolean isPerformanceMode() {
        return performanceMode;
    }
    
    public Collection<Check> getAllChecks() {
        return registeredChecks.values();
    }
}