package net.fliuxx.aFKGuard.gui;

import net.fliuxx.aFKGuard.AFKGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKVerificationGUI {

    private final AFKGuard plugin;
    private final ConcurrentHashMap<UUID, Inventory> openInventories;

    public static final String INVENTORY_TITLE = "Verifica AFK";
    private static final int INVENTORY_SIZE = 27; // 3 rows
    private static final int VERIFICATION_BUTTON_SLOT = 13; // Middle slot

    public AFKVerificationGUI(AFKGuard plugin) {
        this.plugin = plugin;
        this.openInventories = new ConcurrentHashMap<>();
    }

    public void openVerificationGUI(Player player) {
        UUID uuid = player.getUniqueId();

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getVerificationTitle() + " - " + INVENTORY_TITLE));

        // Fill inventory with filler items
        ItemStack fillerItem = createGuiItem(Material.STAINED_GLASS_PANE, (short) 7,
                ChatColor.RED + "Verifica AFK",
                ChatColor.GRAY + "Clicca sul bottone verde per verificarti");

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, fillerItem);
        }

        // Add verification button
        inventory.setItem(VERIFICATION_BUTTON_SLOT, createVerificationButton());

        // Save inventory and open it
        openInventories.put(uuid, inventory);
        player.openInventory(inventory);
    }

    public boolean hasOpenInventory(UUID uuid) {
        return openInventories.containsKey(uuid);
    }

    public void removeInventory(UUID uuid) {
        openInventories.remove(uuid);
    }

    public boolean isVerificationInventory(Inventory inventory) {
        return inventory != null && inventory.getTitle() != null &&
                inventory.getTitle().contains(INVENTORY_TITLE);
    }

    private ItemStack createVerificationButton() {
        return createGuiItem(Material.EMERALD_BLOCK, (short) 0,
                ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getVerificationButtonText()),
                ChatColor.GREEN + "Clicca qui per confermare che sei attivo");
    }

    private ItemStack createGuiItem(Material material, short data, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        if (lore != null && lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }

        item.setItemMeta(meta);
        return item;
    }
}