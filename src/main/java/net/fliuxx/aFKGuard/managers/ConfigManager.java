package net.fliuxx.aFKGuard.managers;

import net.fliuxx.aFKGuard.AFKGuard;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ConfigManager {

    private final AFKGuard plugin;
    private FileConfiguration config;
    private FileConfiguration settingsConfig;
    private File settingsFile;

    public ConfigManager(AFKGuard plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadSettingsConfig();
    }

    private void loadSettingsConfig() {
        settingsFile = new File(plugin.getDataFolder(), "settings.yml");
        if (!settingsFile.exists()) {
            settingsFile.getParentFile().mkdirs();
            plugin.saveResource("settings.yml", false);
        }
        settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);

        InputStream defaultSettingsStream = plugin.getResource("settings.yml");
        if (defaultSettingsStream != null) {
            YamlConfiguration defaultSettings = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultSettingsStream));

            for (String key : defaultSettings.getKeys(true)) {
                if (!settingsConfig.contains(key)) {
                    settingsConfig.set(key, defaultSettings.get(key));
                }
            }

            try {
                settingsConfig.save(settingsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile salvare il file settings.yml: " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        if (settingsFile != null) {
            settingsConfig = YamlConfiguration.loadConfiguration(settingsFile);
        } else {
            loadSettingsConfig();
        }
    }

    public int getAfkTime() {
        return config.getInt("settings.afk-time", 300);
    }

    public int getKickTime() {
        return config.getInt("settings.kick-time", 600);
    }

    public boolean useAfkArea() {
        return config.getBoolean("settings.use-afk-area", false);
    }

    public String getDetectionMethod() {
        return config.getString("settings.detection-method", "ADVANCED");
    }

    public boolean blockCommands() {
        return config.getBoolean("settings.block-commands", true);
    }

    public List<String> getAllowedCommands() {
        return config.getStringList("settings.allowed-commands");
    }

    public boolean broadcastAfkMessages() {
        return config.getBoolean("settings.broadcast-afk-messages", true);
    }

    public double getMinMovementDistance() {
        return settingsConfig.getDouble("detection.advanced.min-movement-distance", 0.05);
    }

    public double getMaxSmallMovement() {
        return settingsConfig.getDouble("detection.advanced.max-small-movement", 2.0);
    }

    public float getMinCameraYaw() {
        return (float) settingsConfig.getDouble("detection.advanced.min-camera-yaw", 5.0);
    }

    public float getMinCameraPitch() {
        return (float) settingsConfig.getDouble("detection.advanced.min-camera-pitch", 5.0);
    }

    public boolean considerRotationInSimple() {
        return settingsConfig.getBoolean("detection.simple.consider-rotation", false);
    }

    public boolean isVerificationEnabled() {
        return config.getBoolean("verification.enabled", true);
    }

    public int getVerificationInterval() {
        return config.getInt("verification.interval", 450);
    }

    public int getVerificationChance() {
        return config.getInt("verification.chance", 65);
    }

    public int getVerificationTimeout() {
        return config.getInt("verification.timeout", 60);
    }

    public String getVerificationTitle() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("verification.title", "&c&lVerifica AFK"));
    }

    public String getVerificationSubtitle() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("verification.subtitle", "&7Clicca sul bottone per dimostrare che sei attivo"));
    }

    public String getVerificationButtonText() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("verification.button-text", "&a&lClicca qui"));
    }

    public String getVerificationTimeoutMessage() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("verification.timeout-message", "&cNon hai risposto alla verifica AFK in tempo."));
    }

    public Location getAfkArea() {
        String worldName = config.getString("afk-area.world", "world");
        World world = plugin.getServer().getWorld(worldName);

        if (world == null) {
            plugin.getLogger().warning("Mondo specificato per l'area AFK non trovato: " + worldName);
            return null;
        }

        double x = config.getDouble("afk-area.x", 0);
        double y = config.getDouble("afk-area.y", 100);
        double z = config.getDouble("afk-area.z", 0);
        float yaw = (float) config.getDouble("afk-area.yaw", 0);
        float pitch = (float) config.getDouble("afk-area.pitch", 0);

        return new Location(world, x, y, z, yaw, pitch);
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.prefix", "&7[&bAFK&fGuard&7] "));
    }

    public String getMessage(String path, String defaultValue) {
        return ChatColor.translateAlternateColorCodes('&',
                config.getString("messages." + path, defaultValue));
    }

    public String getPlayerNowAfkMessage(String playerName) {
        return getMessage("player-now-afk", "&e{player} &7è ora &cAFK&7.")
                .replace("{player}", playerName);
    }

    public String getPlayerNoLongerAfkMessage(String playerName) {
        return getMessage("player-no-longer-afk", "&e{player} &7non è più &cAFK&7.")
                .replace("{player}", playerName);
    }

    public String getYouAreNowAfkMessage() {
        return getMessage("you-are-now-afk", "&7Sei ora in modalità &cAFK&7.");
    }

    public String getYouAreNoLongerAfkMessage() {
        return getMessage("you-are-no-longer-afk", "&7Non sei più in modalità &cAFK&7.");
    }

    public String getKickMessage() {
        return getMessage("kick-message", "&cSei stato disconnesso per inattività prolungata!");
    }

    public String getConfigReloadedMessage() {
        return getMessage("config-reloaded", "&aConfigurazione ricaricata con successo!");
    }

    public String getNoPermissionMessage() {
        return getMessage("no-permission", "&cNon hai i permessi per eseguire questo comando!");
    }
}