package net.fliuxx.aFKGuard.managers;

import net.fliuxx.aFKGuard.AFKGuard;
import net.fliuxx.aFKGuard.gui.AFKVerificationGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VerificationManager {

    private final AFKGuard plugin;
    private final Map<UUID, Long> lastVerificationTimes;
    private final Map<UUID, BukkitTask> pendingVerifications;
    private final Set<UUID> activeVerifications;
    private final AFKVerificationGUI verificationGUI;

    public VerificationManager(AFKGuard plugin) {
        this.plugin = plugin;
        this.lastVerificationTimes = new HashMap<>();
        this.pendingVerifications = new HashMap<>();
        this.activeVerifications = new HashSet<>();
        this.verificationGUI = new AFKVerificationGUI(plugin);
    }

    public boolean shouldSendVerification(UUID uuid) {
        if (!plugin.getConfigManager().isVerificationEnabled()) {
            return false;
        }

        if (!plugin.getAfkManager().isPlayerAFK(uuid)) {
            return false;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || player.hasPermission("afkguard.bypass")) {
            return false;
        }

        if (activeVerifications.contains(uuid)) {
            return false;
        }

        long lastVerificationTime = lastVerificationTimes.getOrDefault(uuid, 0L);
        long currentTime = System.currentTimeMillis();
        long verificationIntervalMillis = plugin.getConfigManager().getVerificationInterval() * 1000L;

        if (currentTime - lastVerificationTime < verificationIntervalMillis) {
            return false;
        }

        int randomValue = ThreadLocalRandom.current().nextInt(100);
        return randomValue < plugin.getConfigManager().getVerificationChance();
    }

    public void sendVerification(Player player) {
        UUID uuid = player.getUniqueId();

        lastVerificationTimes.put(uuid, System.currentTimeMillis());
        activeVerifications.add(uuid);

        // Show title for notification
        player.sendTitle(
                plugin.getConfigManager().getVerificationTitle(),
                plugin.getConfigManager().getVerificationSubtitle()
        );

        // Open the GUI instead of sending a message
        verificationGUI.openVerificationGUI(player);

        // Set timeout for verification
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeVerifications.contains(uuid)) {
                activeVerifications.remove(uuid);
                pendingVerifications.remove(uuid);
                verificationGUI.removeInventory(uuid);

                player.kickPlayer(plugin.getConfigManager().getVerificationTimeoutMessage());
            }
        }, plugin.getConfigManager().getVerificationTimeout() * 20L);

        pendingVerifications.put(uuid, task);
    }

    public boolean completeVerification(Player player) {
        UUID uuid = player.getUniqueId();

        if (!activeVerifications.contains(uuid)) {
            return false;
        }

        BukkitTask task = pendingVerifications.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        activeVerifications.remove(uuid);
        verificationGUI.removeInventory(uuid);

        plugin.getAfkManager().setPlayerAFK(player, false);

        return true;
    }

    public void removePlayer(UUID uuid) {
        lastVerificationTimes.remove(uuid);
        activeVerifications.remove(uuid);
        verificationGUI.removeInventory(uuid);

        BukkitTask task = pendingVerifications.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void cleanup() {
        for (BukkitTask task : pendingVerifications.values()) {
            task.cancel();
        }

        lastVerificationTimes.clear();
        pendingVerifications.clear();
        activeVerifications.clear();
    }

    public AFKVerificationGUI getVerificationGUI() {
        return verificationGUI;
    }

    public boolean hasActiveVerification(UUID uuid) {
        return activeVerifications.contains(uuid);
    }
}