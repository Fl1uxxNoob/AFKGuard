package net.fliuxx.aFKGuard;

import net.fliuxx.aFKGuard.commands.AFKCommand;
import net.fliuxx.aFKGuard.listeners.PlayerActivityListener;
import net.fliuxx.aFKGuard.managers.AFKManager;
import net.fliuxx.aFKGuard.managers.ConfigManager;
import net.fliuxx.aFKGuard.managers.VerificationManager;
import net.fliuxx.aFKGuard.tasks.AFKCheckTask;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class AFKGuard extends JavaPlugin {

    private ConfigManager configManager;
    private AFKManager afkManager;
    private VerificationManager verificationManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        afkManager = new AFKManager(this);
        verificationManager = new VerificationManager(this);

        configManager.loadConfig();

        getCommand("afk").setExecutor(new AFKCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerActivityListener(this), this);

        new AFKCheckTask(this).runTaskTimer(this, 20L, 20L);

        getLogger().info("AFKGuard è stato abilitato con successo!");
    }

    @Override
    public void onDisable() {
        if (afkManager != null) {
            afkManager.cleanup();
        }

        getLogger().info("AFKGuard è stato disabilitato.");
    }

    public String formatMessage(String message) {
        String prefix = configManager.getPrefix();
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AFKManager getAfkManager() {
        return afkManager;
    }

    public VerificationManager getVerificationManager() {
        return verificationManager;
    }
}