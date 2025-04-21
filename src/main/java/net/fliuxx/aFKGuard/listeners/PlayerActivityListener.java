package net.fliuxx.aFKGuard.listeners;

import net.fliuxx.aFKGuard.AFKGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class PlayerActivityListener implements Listener {

    private final AFKGuard plugin;

    public PlayerActivityListener(AFKGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getAfkManager().initializePlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAfkManager().removePlayer(player.getUniqueId());
        plugin.getVerificationManager().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        plugin.getAfkManager().updatePlayerActivity(player, event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        plugin.getAfkManager().updatePlayerActivity(player, player.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        plugin.getAfkManager().updatePlayerActivity(player, player.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getAfkManager().updatePlayerActivity(player, player.getLocation());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (plugin.getAfkManager().isPlayerAFK(player.getUniqueId()) &&
                plugin.getConfigManager().blockCommands() &&
                !player.hasPermission("afkguard.bypass")) {

            String commandUsed = event.getMessage().substring(1).split(" ")[0].toLowerCase();

            if (!plugin.getConfigManager().getAllowedCommands().contains(commandUsed)) {
                event.setCancelled(true);
                player.sendMessage(plugin.formatMessage("&cNon puoi usare comandi mentre sei AFK."));
                return;
            }
        }

        plugin.getAfkManager().updatePlayerActivity(player, player.getLocation());
    }
}