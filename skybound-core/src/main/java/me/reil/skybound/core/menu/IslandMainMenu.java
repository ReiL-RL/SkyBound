package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Main island menu (/is).
 */
public final class IslandMainMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final LangManager lang;

    public IslandMainMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
        this.lang = plugin.getLangManager();
    }

    @Override
    public String getTitle() {
        return msg("menu.main.title", "{island}", island.getName());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Decorative top row (cyan glass)
        ItemStack topGlass = decoration(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            inventory.setItem(i, topGlass);
        }

        // Side columns
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }

        // Decorative bottom row (gray glass)
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        // ════════════ Header (slot 4) — Island info ════════════
        int prestige = plugin.getPrestigeManager().getPrestigeLevel(island.getId());
        int minLevelForPrestige = plugin.getPrestigeManager().getMinLevelToPrestige();
        int currentLevel = island.getLevel();
        int displayCurLevel = Math.min(currentLevel, minLevelForPrestige);
        int prestigeTokens = plugin.getPrestigeShopManager().getTokens(island.getId());

        ItemStack info = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(msg("menu.main.info-title", "{island}", island.getName()));
            List<String> iLore = new ArrayList<String>();
            iLore.add("");
            iLore.add(msg("menu.main.info-level", "{level}", String.valueOf(currentLevel)));
            iLore.add(msg("menu.main.info-value", "{value}", String.format("%.0f", island.getValue())));
            iLore.add(msg("menu.main.info-bank", "{bank}", String.format("%.0f", island.getBankBalance())));
            iLore.add(msg("menu.main.info-members", "{count}", String.valueOf(island.getMembers().size())));
            iLore.add("");
            iLore.add(msg("menu.main.info-prestige",
                    "{prestige}", String.valueOf(prestige),
                    "{current}", String.valueOf(displayCurLevel),
                    "{required}", String.valueOf(minLevelForPrestige)));
            iLore.add(msg("menu.main.info-tokens", "{tokens}", String.valueOf(prestigeTokens)));
            iMeta.setLore(iLore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // ════════════ Row 1 (slots 10-16) ════════════
        boolean addonRegistered = isAddonRegistered();

        // Home (10)
        button(10, Material.OAK_DOOR, msg("menu.main.home"),
                msg("menu.main.home-lore"),
                "",
                msg("menu.main.click-teleport"));

        // Members (12)
        button(12, Material.PLAYER_HEAD, msg("menu.main.members"),
                msg("menu.main.members-lore"),
                msg("menu.main.members-count", "{count}", String.valueOf(island.getMembers().size())),
                "",
                msg("menu.main.click-open"));

        // Upgrades (14)
        if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableUpgradeMenu()) {
            button(14, Material.GRAY_DYE, msg("menu.main.upgrades-disabled"),
                    msg("menu.main.upgrades-disabled-lore1"),
                    msg("menu.main.use-core-lore2"));
        } else {
            button(14, Material.DIAMOND, msg("menu.main.upgrades"),
                    msg("menu.main.upgrades-lore"),
                    "",
                    msg("menu.main.click-open"));
        }

        // Boosters (16)
        if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableBoosterMenu()) {
            button(16, Material.GRAY_DYE, msg("menu.main.boosters-disabled"),
                    msg("menu.main.boosters-disabled-lore1"),
                    msg("menu.main.use-core-lore2"));
        } else {
            button(16, Material.BREWING_STAND, msg("menu.main.boosters"),
                    msg("menu.main.boosters-lore"),
                    "",
                    msg("menu.main.click-open"));
        }

        // ════════════ Row 2 (slots 19-25) ════════════
        button(19, Material.WRITTEN_BOOK, msg("menu.main.missions"),
                msg("menu.main.missions-lore"),
                "",
                msg("menu.main.click-open"));

        button(21, Material.EMERALD, msg("menu.main.shop"),
                msg("menu.main.shop-lore"),
                "",
                msg("menu.main.click-open"));

        button(23, Material.GOLD_INGOT, msg("menu.main.bank"),
                msg("menu.main.bank-lore"),
                msg("menu.main.bank-current", "{bank}", String.format("%.0f", island.getBankBalance())),
                "",
                msg("menu.main.click-open"));

        button(25, Material.NETHER_STAR, msg("menu.main.top"),
                msg("menu.main.top-lore"),
                "",
                msg("menu.main.click-open"));

        // Built-in XP core (slot 22) — center between Magazin and Bank
        if (!addonRegistered) {
            button(22, Material.EXPERIENCE_BOTTLE, msg("menu.main.xp-core"),
                    msg("menu.main.xp-core-lore1"),
                    msg("menu.main.xp-core-lore2"),
                    msg("menu.main.xp-core-lore3"),
                    msg("menu.main.xp-core-lore4"),
                    "",
                    msg("menu.main.click-open"));
        }

        // ════════════ Row 3 (slots 28-34) ════════════
        button(28, Material.NETHER_STAR, msg("menu.main.prestige-shop"),
                msg("menu.main.prestige-shop-lore"),
                msg("menu.main.tokens-current", "{tokens}", String.valueOf(prestigeTokens)),
                "",
                msg("menu.main.click-open"));

        button(30, Material.ENDER_PEARL, msg("menu.main.warps"),
                msg("menu.main.warps-lore"),
                msg("menu.main.warps-count", "{count}", String.valueOf(island.getWarps().size())),
                "",
                msg("menu.main.click-open"));

        button(32, Material.COMPARATOR, msg("menu.main.settings"),
                msg("menu.main.settings-lore"),
                "",
                msg("menu.main.click-open"));

        // Events button (only if VoidRift is installed)
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("VoidRift")) {
            button(34, Material.ENDER_EYE, msg("menu.main.events"),
                    msg("menu.main.events-lore"),
                    "",
                    msg("menu.main.click-open"));
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        boolean addonRegistered = isAddonRegistered();

        switch (slot) {
            case 10:
                player.closeInventory();
                player.teleport(island.getHome());
                lang.send(player, "island.teleported-home");
                break;
            case 12:
                new IslandMembersMenu(player, plugin, island).withParent(this).open();
                break;
            case 14:
                if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableUpgradeMenu()) {
                    lang.send(player, "island-core.upgrade-via-core");
                } else {
                    new UpgradesMenu(player, plugin, island).withParent(this).open();
                }
                break;
            case 16:
                if (addonRegistered && plugin.getCoreConfig().isIslandCoreDisableBoosterMenu()) {
                    lang.send(player, "island-core.booster-via-core");
                } else {
                    new BoostersMenu(player, plugin, island).withParent(this).open();
                }
                break;
            case 19:
                new MissionCategoryMenu(player, plugin).withParent(this).open();
                break;
            case 21:
                new ShopCategoryMenu(player, plugin).withParent(this).open();
                break;
            case 22:
                if (!addonRegistered) {
                    plugin.getIslandValueMenu().open(player);
                }
                break;
            case 23:
                new BankMenu(player, plugin, island).withParent(this).open();
                break;
            case 25:
                new TopIslandsMenu(player, plugin).withParent(this).open();
                break;
            case 28:
                new PrestigeShopMenu(player, plugin).withParent(this).open();
                break;
            case 30:
                new WarpsMenu(player, plugin, island).withParent(this).open();
                break;
            case 32:
                new IslandSettingsMenu(player, plugin, island).withParent(this).open();
                break;
            case 34:
                new EventsMenu(player, plugin).withParent(this).open();
                break;
        }
    }

    private boolean isAddonRegistered() {
        try {
            return plugin.getAddonRegistry().isRegistered("island-core");
        } catch (Exception e) {
            return false;
        }
    }

    private void button(int slot, Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<String>();
                for (String line : lore) loreList.add(line);
                meta.setLore(loreList);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private ItemStack decoration(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String msg(String key, String... replacements) {
        return lang.get(key, replacements);
    }
}
