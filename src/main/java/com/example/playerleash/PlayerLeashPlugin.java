package com.example.playerleash;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class PlayerLeashPlugin extends JavaPlugin {

    private LeashManager leashManager;
    private BukkitTask effectTask;

    @Override
    public void onEnable() {
        leashManager = new LeashManager(this);
        getServer().getPluginManager().registerEvents(new LeashListener(leashManager, this), this);
        
        effectTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (leashManager != null) {
                leashManager.updateEffects();
            }
        }, 5L, 5L);
        
        getLogger().info("PlayerLeash plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (effectTask != null) {
            effectTask.cancel();
        }
        
        if (leashManager != null) {
            leashManager.clearAllLeashes();
        }
        getLogger().info("PlayerLeash plugin has been disabled!");
    }

    public LeashManager getLeashManager() {
        return leashManager;
    }
}

