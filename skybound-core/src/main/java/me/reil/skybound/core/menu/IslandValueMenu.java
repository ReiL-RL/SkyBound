package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in deposit GUI used when the island-core addon is NOT installed.
 *
 * Slots 0-8: information about accepted blocks (auto-generated from config.yml block-values)
 * Slots 9-17: deposit area — players place items here
 * Slot 22: Confirm — converts items into XP/Value and closes
 * Slot 26: Cancel  — returns items
 *
 * Each item type contributes its block-value (config.yml block-values.MATERIAL) to:
 *  - island.addExperience(value)  (gives XP / level up)
 *  - island.addValue(value)       (raises island worth)
 */
public final class IslandValueMenu implements Listener {

    private static final int SIZE = 27;

    private static final int CONFIRM_SLOT = 22;
    private static final int CANCEL_SLOT = 26;
    private static final int DEPOSIT_START = 9;
    private static final int DEPOSIT_END = 17;

    private final SkyBoundPlugin plugin;
    /** Players currently viewing the menu. */
    private final java.util.Set<UUID> viewers = new java.util.HashSet<UUID>();

    public IslandValueMenu(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder(), SIZE, plugin.getLangManager().get("menu.value.title"));

        Map<String, Integer> values = plugin.getCoreConfig().getBlockValues();

        // Info row 0..8 with examples of accepted materials
        int slot = 0;
        for (Map.Entry<String, Integer> e : values.entrySet()) {
            if (slot > 8) break;
            Material mat;
            try { mat = Material.valueOf(e.getKey().toUpperCase()); }
            catch (IllegalArgumentException ex) { continue; }

            ItemStack info = new ItemStack(mat);
            ItemMeta meta = info.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(plugin.getLangManager().get("menu.value.resource-title",
                        "{material}", mat.name(), "{value}", String.valueOf(e.getValue())));
                List<String> lore = new ArrayList<String>();
                lore.add(plugin.getLangManager().get("menu.value.resource-lore1"));
                lore.add(plugin.getLangManager().get("menu.value.resource-lore2", "{value}", String.valueOf(e.getValue())));
                lore.add(plugin.getLangManager().get("menu.value.resource-lore3", "{value}", String.valueOf(e.getValue())));
                meta.setLore(lore);
                info.setItemMeta(meta);
            }
            inv.setItem(slot, info);
            slot++;
        }
        while (slot <= 8) {
            inv.setItem(slot, filler(Material.GRAY_STAINED_GLASS_PANE, " "));
            slot++;
        }

        // Bottom row: filler around buttons
        for (int i = 18; i < SIZE; i++) {
            if (i == CONFIRM_SLOT || i == CANCEL_SLOT) continue;
            inv.setItem(i, filler(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        inv.setItem(CONFIRM_SLOT, filler(Material.LIME_STAINED_GLASS_PANE, plugin.getLangManager().get("menu.value.confirm")));
        inv.setItem(CANCEL_SLOT, filler(Material.RED_STAINED_GLASS_PANE, plugin.getLangManager().get("menu.value.cancel")));

        player.openInventory(inv);
        viewers.add(player.getUniqueId());
    }

    private ItemStack filler(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof Holder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player p = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        int topSize = top.getSize();

        // Allow free interaction with deposit slots and player inventory below
        if (slot >= DEPOSIT_START && slot <= DEPOSIT_END) return;
        if (slot >= topSize) return; // player inventory

        // Block clicks on info row
        if (slot >= 0 && slot <= 8) {
            event.setCancelled(true);
            return;
        }

        if (slot == CONFIRM_SLOT) {
            event.setCancelled(true);
            handleConfirm(p, top);
            return;
        }
        if (slot == CANCEL_SLOT) {
            event.setCancelled(true);
            handleCancel(p, top);
            return;
        }
        // Decoration row
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();
        if (!(top.getHolder() instanceof Holder)) return;
        if (!(event.getPlayer() instanceof Player)) return;

        Player p = (Player) event.getPlayer();
        if (!viewers.remove(p.getUniqueId())) return;

        // Return any items that were left in the deposit slots
        returnDepositItems(p, top);
    }

    private void handleConfirm(Player player, Inventory inv) {
        Map<String, Integer> values = plugin.getCoreConfig().getBlockValues();

        long totalAmount = 0L;
        int itemsTaken = 0;

        for (int i = DEPOSIT_START; i <= DEPOSIT_END; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            Integer perItem = values.get(item.getType().name());
            if (perItem == null || perItem <= 0) {
                // Not a valid item — return it
                continue;
            }
            int count = item.getAmount();
            totalAmount += (long) perItem * (long) count;
            itemsTaken += count;
            inv.setItem(i, null);
        }

        if (totalAmount <= 0) {
            plugin.getLangManager().send(player, "value.no-resources");
            return;
        }

        Island island = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
        if (island == null) {
            plugin.getLangManager().send(player, "island.no-island");
            return;
        }

        island.addExperience(totalAmount);
        island.addValue(totalAmount);

        plugin.getLangManager().send(player, "value.deposited",
                "{items}", String.valueOf(itemsTaken),
                "{xp}", String.valueOf(totalAmount),
                "{value}", String.valueOf(totalAmount));

        // Return any leftover items (non-accepted ones) to the player
        returnDepositItems(player, inv);
    }

    private void handleCancel(Player player, Inventory inv) {
        returnDepositItems(player, inv);
        player.closeInventory();
    }

    private void returnDepositItems(Player player, Inventory inv) {
        for (int i = DEPOSIT_START; i <= DEPOSIT_END; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir()) continue;
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(it);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            inv.setItem(i, null);
        }
    }

    /** Marker holder so we can identify our inventory in click events. */
    public static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
