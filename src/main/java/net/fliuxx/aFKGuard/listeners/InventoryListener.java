package net.fliuxx.aFKGuard.listeners;

import net.fliuxx.aFKGuard.AFKGuard;
import net.fliuxx.aFKGuard.gui.AFKVerificationGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class InventoryListener implements Listener {

    private final AFKGuard plugin;

    public InventoryListener(AFKGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Check if it's our verification GUI
        if (plugin.getVerificationManager().getVerificationGUI().isVerificationInventory(event.getInventory())) {
            event.setCancelled(true); // Cancel all inventory interactions

            // Check if they clicked the verification button (slot 13)
            if (event.getSlot() == 13) {
                plugin.getVerificationManager().completeVerification(player);
                player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // If it's our verification GUI and verification is still active
        if (plugin.getVerificationManager().getVerificationGUI().isVerificationInventory(event.getInventory()) &&
                plugin.getVerificationManager().hasActiveVerification(player.getUniqueId())) {

            // Re-open the inventory after a tick unless verification is completed
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && plugin.getVerificationManager().hasActiveVerification(player.getUniqueId())) {
                        plugin.getVerificationManager().getVerificationGUI().openVerificationGUI(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }
}