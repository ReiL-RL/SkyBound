package me.reil.skybound.core.island;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared island chest accessible by all members from anywhere.
 * Each island gets a 27-slot shared inventory.
 */
public final class IslandChestManager {

    private final Map<String, Inventory> chests = new LinkedHashMap<String, Inventory>();

    /**
     * Open the island chest for a player.
     */
    public void open(Player player, String islandId, String islandName) {
        Inventory chest = chests.get(islandId);
        if (chest == null) {
            chest = Bukkit.createInventory(null, 27, ChatColor.DARK_AQUA + "Island Chest - " + islandName);
            chests.put(islandId, chest);
        }
        player.openInventory(chest);
    }

    /**
     * Get the chest inventory for persistence.
     */
    public Inventory getChest(String islandId) {
        return chests.get(islandId);
    }

    /**
     * Set a chest inventory (for loading from persistence).
     */
    public void setChest(String islandId, Inventory inventory) {
        chests.put(islandId, inventory);
    }

    /**
     * Remove a chest (when island is deleted).
     */
    public void removeChest(String islandId) {
        chests.remove(islandId);
    }
}
