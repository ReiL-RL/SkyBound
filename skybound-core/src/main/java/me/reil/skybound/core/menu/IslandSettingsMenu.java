package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Full island settings menu.
 * All island management accessible via GUI:
 * - Lock/Unlock
 * - Rename
 * - Change biome
 * - Toggle autosell
 * - Role permissions (global for island)
 * - Island prestige
 * - Regenerate
 * - Delete
 */
public final class IslandSettingsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;

    public IslandSettingsMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override
    public String getTitle() {
        return lang().get("menu.settings.title");
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Fill border
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 36; i < 45; i++) inventory.setItem(i, border);

        boolean locked = island.isLocked();
        boolean autosell = plugin.getAutosellListener().isAutosellEnabled(player.getUniqueId());
        int prestige = plugin.getPrestigeManager().getPrestigeLevel(island.getId());

        // Row 1: Basic settings
        // Lock/Unlock (slot 10)
        inventory.setItem(10, makeToggleItem(
                locked ? Material.RED_WOOL : Material.GREEN_WOOL,
                locked ? lang().get("menu.settings.lock") : lang().get("menu.settings.unlock"),
                locked ? lang().get("menu.settings.lock-lore") : lang().get("menu.settings.unlock-lore")));

        // Rename (slot 12)
        inventory.setItem(12, makeActionItem(Material.NAME_TAG, lang().get("menu.settings.rename"),
                lang().get("menu.settings.rename-lore", "{name}", island.getName())));

        // Description (slot 13)
        inventory.setItem(13, makeActionItem(Material.WRITABLE_BOOK, lang().get("menu.settings.description"),
                lang().get("menu.settings.description-lore", "{desc}", island.getDescription().isEmpty() ? "-" : island.getDescription())));

        // Biome (slot 14)
        inventory.setItem(14, makeActionItem(Material.GRASS_BLOCK, lang().get("menu.settings.biome"),
                lang().get("menu.settings.biome-lore")));

        // Autosell (slot 16)
        inventory.setItem(16, makeToggleItem(
                autosell ? Material.HOPPER : Material.HOPPER_MINECART,
                autosell ? lang().get("menu.settings.autosell-on") : lang().get("menu.settings.autosell-off"),
                lang().get("menu.settings.autosell-lore")));

        // Row 2: Advanced
        // Role Permissions (slot 19)
        inventory.setItem(19, makeActionItem(Material.IRON_BARS, lang().get("menu.settings.role-perms"),
                lang().get("menu.settings.role-perms-lore")));

        // Warps (slot 21)
        inventory.setItem(21, makeActionItem(Material.ENDER_PEARL, lang().get("menu.settings.warps"),
                lang().get("menu.settings.warps-lore", "{count}", String.valueOf(island.getWarps().size()), "{max}", String.valueOf(plugin.getCoreConfig().getMaxWarps()))));

        // Island Chest (slot 22)
        inventory.setItem(22, makeActionItem(Material.ENDER_CHEST, lang().get("menu.settings.chest"),
                lang().get("menu.settings.chest-lore")));

        // Logs (slot 23)
        inventory.setItem(23, makeActionItem(Material.BOOK, lang().get("menu.settings.logs"),
                lang().get("menu.settings.logs-lore")));

        // Prestige (slot 25)
        inventory.setItem(25, makeActionItem(Material.NETHER_STAR, lang().get("menu.settings.prestige"),
                lang().get("menu.settings.prestige-lore", "{level}", String.valueOf(prestige), "{max}", String.valueOf(plugin.getPrestigeManager().getMaxPrestige()), "{percent}", String.format("%.0f", (plugin.getPrestigeManager().getMultiplier(island.getId()) - 1.0) * 100))));

        // Row 3: Danger zone
        // Regenerate (slot 30)
        inventory.setItem(30, makeActionItem(Material.TNT, lang().get("menu.settings.regen"),
                lang().get("menu.settings.regen-lore")));

        // Delete (slot 32)
        inventory.setItem(32, makeActionItem(Material.LAVA_BUCKET, lang().get("menu.settings.delete"),
                lang().get("menu.settings.delete-lore")));

        // Back (slot 40)
        inventory.setItem(40, makeItem(Material.ARROW, lang().get("button.back")));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        switch (slot) {
            case 10: // Lock toggle
                island.setLocked(!island.isLocked());
                plugin.getIslandLogManager().log(island.getId(), player.getUniqueId(), player.getName(),
                        me.reil.skybound.core.island.IslandLogEntry.LogAction.ISLAND_LOCK, island.isLocked() ? "locked" : "unlocked");
                new IslandSettingsMenu(player, plugin, island).open();
                break;

            case 12: // Rename
                player.closeInventory();
                lang().send(player, "menu.settings.rename-lore", "{name}", island.getName());
                break;

            case 13: // Description
                player.closeInventory();
                lang().send(player, "menu.settings.description-lore", "{desc}", island.getDescription());
                break;

            case 14: // Biome
                new BiomeSelectMenu(player, plugin, island).open();
                break;

            case 16: // Autosell toggle
                plugin.getAutosellListener().toggleAutosell(player.getUniqueId());
                new IslandSettingsMenu(player, plugin, island).open();
                break;

            case 19: // Role permissions
                new RolePermissionsMenu(player, plugin, island).open();
                break;

            case 21: // Warps
                new WarpsMenu(player, plugin, island).open();
                break;

            case 22: // Island chest
                player.closeInventory();
                plugin.getIslandChestManager().open(player, island.getId(), island.getName());
                break;

            case 23: // Logs
                player.closeInventory();
                java.util.List<me.reil.skybound.core.island.IslandLogEntry> logs = plugin.getIslandLogManager().getLogs(island.getId(), 10);
                if (logs.isEmpty()) {
                    lang().send(player, "logs.empty");
                } else {
                    lang().send(player, "logs.header");
                    for (me.reil.skybound.core.island.IslandLogEntry entry : logs) {
                        long ago = (System.currentTimeMillis() - entry.getTimestamp()) / 1000L;
                        String timeStr = ago < 60 ? ago + "s" : (ago < 3600 ? (ago / 60) + "m" : (ago / 3600) + "h");
                        lang().send(player, "logs.entry", "{time}", timeStr, "{player}", entry.getPlayerName(), "{action}", entry.getAction().name().toLowerCase());
                    }
                }
                break;

            case 25: // Prestige
                player.closeInventory();
                if (!plugin.getPrestigeManager().canPrestige(island)) {
                    lang().send(player, "prestige.cannot-level", "{level}", String.valueOf(plugin.getPrestigeManager().getMinLevelToPrestige()), "{current}", String.valueOf(island.getLevel()));
                } else {
                    lang().send(player, "confirm.prestige");
                }
                break;

            case 30: // Regenerate
                player.closeInventory();
                lang().send(player, "confirm.regen");
                break;

            case 32: // Delete
                player.closeInventory();
                lang().send(player, "confirm.delete");
                break;

            case 40: // Back
                new IslandMainMenu(player, plugin, island).open();
                break;
        }
    }

    private String formatAction(me.reil.skybound.core.island.IslandLogEntry.LogAction action) {
        switch (action) {
            case MEMBER_JOIN: return "joined";
            case MEMBER_LEAVE: return "left";
            case MEMBER_KICK: return "was kicked";
            case UPGRADE_PURCHASE: return "bought upgrade";
            case BOOSTER_PURCHASE: return "activated booster";
            case BANK_DEPOSIT: return "deposited";
            case BANK_WITHDRAW: return "withdrew";
            case WARP_SET: return "set warp";
            case WARP_REMOVE: return "removed warp";
            case SETTINGS_CHANGE: return "changed settings";
            case ISLAND_LOCK: return "toggled lock";
            case ISLAND_UNLOCK: return "toggled lock";
            case ISLAND_RENAME: return "renamed island";
            default: return action.name();
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

    private ItemStack makeToggleItem(Material material, String name, String... loreLines) {
        return makeActionItem(material, name, loreLines);
    }

    private ItemStack makeActionItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<String>();
            for (String line : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
