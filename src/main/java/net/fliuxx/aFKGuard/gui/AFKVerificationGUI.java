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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AFKVerificationGUI {

    private final AFKGuard plugin;
    private final ConcurrentHashMap<UUID, Inventory> openInventories;
    private final ConcurrentHashMap<UUID, Integer> buttonPositions;
    private final Random random;

    public static final String INVENTORY_TITLE = "Verifica AFK";
    private static final int INVENTORY_SIZE = 27; // 3 rows

    public AFKVerificationGUI(AFKGuard plugin) {
        this.plugin = plugin;
        this.openInventories = new ConcurrentHashMap<>();
        this.buttonPositions = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    public void openVerificationGUI(Player player) {
        UUID uuid = player.getUniqueId();

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfigManager().getVerificationTitle() + " - " + INVENTORY_TITLE));

        // Fill inventory with filler items
        ItemStack fillerItem = createGuiItem(
                Material.STAINED_GLASS_PANE,
                (short) 7,
                ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getVerificationFillerText()),
                ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getVerificationFillerDescription())
        );

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, fillerItem);
        }

        // Generate random position for verification button
        int buttonPosition = getRandomButtonPosition();
        buttonPositions.put(uuid, buttonPosition);

        // Add verification button at random position
        inventory.setItem(buttonPosition, createVerificationButton());

        // Save inventory and open it
        openInventories.put(uuid, inventory);
        player.openInventory(inventory);
    }

    public boolean hasOpenInventory(UUID uuid) {
        return openInventories.containsKey(uuid);
    }

    public void removeInventory(UUID uuid) {
        openInventories.remove(uuid);
        buttonPositions.remove(uuid);
    }

    public boolean isVerificationInventory(Inventory inventory) {
        return inventory != null && inventory.getTitle() != null &&
                inventory.getTitle().contains(INVENTORY_TITLE);
    }

    public int getButtonPosition(UUID uuid) {
        return buttonPositions.getOrDefault(uuid, -1);
    }

    private int getRandomButtonPosition() {
        // Generate a random slot position, avoiding borders for a better visual experience
        int[] validPositions = {
                4, 10, 11, 12, 13, 14, 15, 16, 22
        };
        return validPositions[random.nextInt(validPositions.length)];
    }

    private ItemStack createVerificationButton() {
        short buttonColor = (short) plugin.getConfigManager().getVerificationButtonColor();
        return createGuiItem(
                Material.STAINED_GLASS,
                buttonColor,
                ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getVerificationButtonText()),
                ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getVerificationButtonDescription())
        );
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