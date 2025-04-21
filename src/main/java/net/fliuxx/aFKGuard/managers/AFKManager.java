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

    public AFKManager(AFKGuard plugin) {
        this.plugin = plugin;
        this.playerActivities = new HashMap<>();
        this.afkStatus = new HashMap<>();
        this.lastActiveTimes = new HashMap<>();
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
        boolean significantMovement = activity.updateLocation(newLocation, plugin.getConfigManager().getDetectionMethod());

        if (significantMovement) {
            lastActiveTimes.put(uuid, System.currentTimeMillis());

            if (afkStatus.getOrDefault(uuid, false)) {
                setPlayerAFK(player, false);
            }
        }
    }

    public void setPlayerAFK(Player player, boolean afk) {
        UUID uuid = player.getUniqueId();

        if (afkStatus.getOrDefault(uuid, false) == afk) {
            return;
        }

        afkStatus.put(uuid, afk);

        if (afk) {
            player.sendMessage(plugin.formatMessage(plugin.getConfigManager().getYouAreNowAfkMessage()));

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.sendMessage(plugin.formatMessage(
                            plugin.getConfigManager().getPlayerNowAfkMessage(player.getName())));
                }
            }

            if (plugin.getConfigManager().useAfkArea()) {
                Location afkArea = plugin.getConfigManager().getAfkArea();
                if (afkArea != null) {
                    player.teleport(afkArea);
                }
            }
        } else {
            player.sendMessage(plugin.formatMessage(plugin.getConfigManager().getYouAreNoLongerAfkMessage()));

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(player)) {
                    onlinePlayer.sendMessage(plugin.formatMessage(
                            plugin.getConfigManager().getPlayerNoLongerAfkMessage(player.getName())));
                }
            }

            // Aggiorna il tempo dell'ultima attività
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
            player.kickPlayer(plugin.getConfigManager().getKickMessage());
        }
    }

    public void removePlayer(UUID uuid) {
        playerActivities.remove(uuid);
        afkStatus.remove(uuid);
        lastActiveTimes.remove(uuid);
    }

    public void cleanup() {
        playerActivities.clear();
        afkStatus.clear();
        lastActiveTimes.clear();
    }

    public int getInactiveTime(UUID uuid) {
        if (!lastActiveTimes.containsKey(uuid)) {
            return 0;
        }

        long timeSinceLastActivity = System.currentTimeMillis() - lastActiveTimes.get(uuid);
        return (int) (timeSinceLastActivity / 1000);
    }
}