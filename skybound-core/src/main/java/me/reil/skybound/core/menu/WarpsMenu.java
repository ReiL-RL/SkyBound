package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Warps management menu.
 * Left-click to teleport, right-click to delete.
 * Shows a "Set Warp" button if under limit.
 */
public final class WarpsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final List<String> warpNames = new ArrayList<String>();

    public WarpsMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override
    public String getTitle() {
        return lang().get("menu.warps.title");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);

        Map<String, Location> warps = island.getWarps();
        int slot = 9;
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            if (slot >= 17) break;
            warpNames.add(entry.getKey());

            ItemStack item = new ItemStack(Material.ENDER_PEARL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + entry.getKey());
                List<String> lore = new ArrayList<String>();
                Location loc = entry.getValue();
                lore.add(lang().get("menu.warps.location", "{x}", String.valueOf((int) loc.getX()), "{y}", String.valueOf((int) loc.getY()), "{z}", String.valueOf((int) loc.getZ())));
                lore.add("");
                lore.add(lang().get("menu.warps.teleport"));
                lore.add(lang().get("menu.warps.delete"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        // Set warp button (slot 17)
        int maxWarps = plugin.getCoreConfig().getMaxWarps();
        if (warps.size() < maxWarps) {
            ItemStack setWarp = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = setWarp.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(lang().get("menu.warps.set-new"));
                List<String> lore = new ArrayList<String>();
                lore.add(lang().get("menu.warps.set-new-lore", "{count}", String.valueOf(warps.size()), "{max}", String.valueOf(maxWarps)));
                meta.setLore(lore);
                setWarp.setItemMeta(meta);
            }
            inventory.setItem(17, setWarp);
        }

        // Back
        inventory.setItem(22, makeItem(Material.ARROW, lang().get("button.back")));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 22) {
            new IslandSettingsMenu(player, plugin, island).open();
            return;
        }

        if (slot == 17) {
            // Set warp
            player.closeInventory();
            if (!island.isWithinBounds(player.getLocation())) {
                player.sendMessage(ChatColor.RED + "You must be on your island to set a warp.");
                return;
            }
            String warpName = "warp_" + (island.getWarps().size() + 1);
            island.setWarp(warpName, player.getLocation());
            player.sendMessage(ChatColor.GREEN + "Warp set: " + warpName + ". Rename with /is setwarp <name>.");
            return;
        }

        int index = slot - 9;
        if (index < 0 || index >= warpNames.size()) return;

        String warpName = warpNames.get(index);

        if (event.getClick() == ClickType.LEFT) {
            // Teleport
            Location loc = island.getWarps().get(warpName);
            if (loc != null) {
                player.closeInventory();
                player.teleport(loc);
                player.sendMessage(ChatColor.GREEN + "Teleported to warp: " + warpName);
            }
        } else if (event.getClick() == ClickType.RIGHT) {
            // Delete
            island.removeWarp(warpName);
            player.sendMessage(ChatColor.RED + "Warp deleted: " + warpName);
            new WarpsMenu(player, plugin, island).open();
        }
    }

    private ItemStack makeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
