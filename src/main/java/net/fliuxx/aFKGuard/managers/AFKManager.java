package net.fliuxx.aFKGuard.managers;

import net.fliuxx.aFKGuard.AFKGuard;
import net.fliuxx.aFKGuard.models.PlayerActivity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AFKManager {

    private final AFKGuard plugin;
    private final Map<UUID, PlayerActivity> playerActivities;
    private final Map<UUID, Boolean> afkStatus;
    private final Map<UUID, Long> lastActiveTimes;
    private final Map<UUID, Long> afkStartTimes;

    public AFKManager(AFKGuard plugin) {
        this.plugin = plugin;
        this.playerActivities = new HashMap<>();
        this.afkStatus = new HashMap<>();
        this.lastActiveTimes = new HashMap<>();
        this.afkStartTimes = new HashMap<>();
    }

    public void initializePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playerActivities.put(uuid, new PlayerActivity(player.getLocation()));
        afkStatus.put(uuid, false);
        lastActiveTimes.put(uuid, System.currentTimeMillis());
    }

    public void updatePlayerActivity(Player player, Location newLocation) {
        UUID uuid = player.getUniqueId();

        if (!playerActivities.containsKey(uuid)) {
            initializePlayer(player);
            return;
        }

        PlayerActivity activity = playerActivities.get(uuid);
        ConfigManager configManager = plugin.getConfigManager();

        boolean significantMovement = activity.updateLocation(
                newLocation,
                configManager.getDetectionMethod(),
                configManager.getMinMovementDistance(),
                configManager.getMaxSmallMovement(),
                configManager.getMinCameraYaw(),
                configManager.getMinCameraPitch(),
                configManager.considerRotationInSimple()
        );

        if (significantMovement) {
            lastActiveTimes.put(uuid, System.currentTimeMillis());

            if (afkStatus.getOrDefault(uuid, false)) {
                setPlayerAFK(player, false);
            }
        }
    }

    public void setPlayerAFK(Player player, boolean afk) {
        UUID uuid = player.getUniqueId();
        boolean wasAfk = afkStatus.getOrDefault(uuid, false);

        if (wasAfk == afk) {
            return;
        }

        afkStatus.put(uuid, afk);
        ConfigManager configManager = plugin.getConfigManager();
        boolean broadcastMessages = configManager.broadcastAfkMessages();

        if (afk) {
            afkStartTimes.put(uuid, System.currentTimeMillis());

            player.sendMessage(plugin.formatMessage(configManager.getYouAreNowAfkMessage()));

            if (broadcastMessages) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        onlinePlayer.sendMessage(plugin.formatMessage(
                                configManager.getPlayerNowAfkMessage(player.getName())));
                    }
                }
            }

            if (!Bukkit.isPrimaryThread() || !wasCommandExecution()) {
                int inactiveTime = getInactiveTime(uuid);
                plugin.getDatabaseManager().logAutoAFK(player, inactiveTime);
            }

            if (configManager.useAfkArea()) {
                Location afkArea = configManager.getAfkArea();
                if (afkArea != null) {
                    player.teleport(afkArea);

                    plugin.getDatabaseManager().logAFKTeleport(player,
                            afkArea.getWorld().getName(),
                            afkArea.getX(),
                            afkArea.getY(),
                            afkArea.getZ());
                }
            }
        } else {
            afkStartTimes.remove(uuid);
            player.sendMessage(plugin.formatMessage(configManager.getYouAreNoLongerAfkMessage()));

            if (broadcastMessages) {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        onlinePlayer.sendMessage(plugin.formatMessage(
                                configManager.getPlayerNoLongerAfkMessage(player.getName())));
                    }
                }
            }

            lastActiveTimes.put(uuid, System.currentTimeMillis());
        }
    }

    public boolean isPlayerAFK(UUID uuid) {
        return afkStatus.getOrDefault(uuid, false);
    }

    public boolean shouldBeAFK(UUID uuid) {
        if (!lastActiveTimes.containsKey(uuid)) {
            return false;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.hasPermission("afkguard.bypass")) {
            return false;
        }

        long timeSinceLastActivity = System.currentTimeMillis() - lastActiveTimes.get(uuid);
        long afkTimeMillis = plugin.getConfigManager().getAfkTime() * 1000L;

        return timeSinceLastActivity >= afkTimeMillis;
    }

    public boolean shouldBeKicked(UUID uuid) {
        if (!afkStatus.getOrDefault(uuid, false) || !lastActiveTimes.containsKey(uuid)) {
            return false;
        }

        if (plugin.getConfigManager().getKickTime() <= 0) {
            return false;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.hasPermission("afkguard.bypass")) {
            return false;
        }

        long timeSinceLastActivity = System.currentTimeMillis() - lastActiveTimes.get(uuid);
        long kickTimeMillis = plugin.getConfigManager().getKickTime() * 1000L;

        return timeSinceLastActivity >= kickTimeMillis;
    }

    public void kickAFKPlayer(Player player) {
        if (player != null && player.isOnline()) {
            UUID uuid = player.getUniqueId();
            int totalAfkTime = getTotalAfkTime(uuid);

            plugin.getDatabaseManager().logAFKKick(player, totalAfkTime);

            player.kickPlayer(plugin.getConfigManager().getKickMessage());
        }
    }

    public void removePlayer(UUID uuid) {
        playerActivities.remove(uuid);
        afkStatus.remove(uuid);
        lastActiveTimes.remove(uuid);
        afkStartTimes.remove(uuid);
    }

    public void cleanup() {
        playerActivities.clear();
        afkStatus.clear();
        lastActiveTimes.clear();
        afkStartTimes.clear();
    }

    public int getInactiveTime(UUID uuid) {
        if (!lastActiveTimes.containsKey(uuid)) {
            return 0;
        }

        long timeSinceLastActivity = System.currentTimeMillis() - lastActiveTimes.get(uuid);
        return (int) (timeSinceLastActivity / 1000);
    }

    public int getTotalAfkTime(UUID uuid) {
        if (!afkStartTimes.containsKey(uuid)) {
            return 0;
        }

        long afkDuration = System.currentTimeMillis() - afkStartTimes.get(uuid);
        return (int) (afkDuration / 1000);
    }

    private boolean wasCommandExecution() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().contains("AFKCommand")) {
                return true;
            }
        }
        return false;
    }
}