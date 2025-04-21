package net.fliuxx.aFKGuard.tasks;

import net.fliuxx.aFKGuard.AFKGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class AFKCheckTask extends BukkitRunnable {

    private final AFKGuard plugin;

    public AFKCheckTask(AFKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            if (!plugin.getAfkManager().isPlayerAFK(uuid) && plugin.getAfkManager().shouldBeAFK(uuid)) {
                plugin.getAfkManager().setPlayerAFK(player, true);
                continue;
            }

            if (plugin.getAfkManager().isPlayerAFK(uuid) && plugin.getAfkManager().shouldBeKicked(uuid)) {
                plugin.getAfkManager().kickAFKPlayer(player);
                continue;
            }

            if (plugin.getAfkManager().isPlayerAFK(uuid) && plugin.getVerificationManager().shouldSendVerification(uuid)) {
                plugin.getVerificationManager().sendVerification(player);
            }
        }
    }
}