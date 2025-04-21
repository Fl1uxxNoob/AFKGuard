package net.fliuxx.aFKGuard.managers;

import net.fliuxx.aFKGuard.AFKGuard;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

    public VerificationManager(AFKGuard plugin) {
        this.plugin = plugin;
        this.lastVerificationTimes = new HashMap<>();
        this.pendingVerifications = new HashMap<>();
        this.activeVerifications = new HashSet<>();
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

        player.sendTitle(
                plugin.getConfigManager().getVerificationTitle(),
                plugin.getConfigManager().getVerificationSubtitle()
        );

        TextComponent message = new TextComponent(plugin.getConfigManager().getVerificationButtonText());
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/afk verify"));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Clicca per confermare che sei attivo").create()));

        player.spigot().sendMessage(message);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeVerifications.contains(uuid)) {
                activeVerifications.remove(uuid);
                pendingVerifications.remove(uuid);

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

        plugin.getAfkManager().setPlayerAFK(player, false);

        return true;
    }

    public void removePlayer(UUID uuid) {
        lastVerificationTimes.remove(uuid);
        activeVerifications.remove(uuid);

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
}