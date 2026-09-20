package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.upgrade.Upgrade;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class UpgradesMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final List<String> upgradeIds = new ArrayList<String>();

    public UpgradesMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override public String getTitle() { return lang().get("menu.upgrades.title"); }
    @Override public int getSize() { return 45; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        upgradeIds.clear();

        // Borders
        ItemStack topGlass = decoration(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 3; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inventory.setItem(i, bottomGlass);

        // Header
        ItemStack info = new ItemStack(Material.DIAMOND);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(lang().get("menu.upgrades.header"));
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(lang().get("menu.upgrades.info1"));
            lore.add(lang().get("menu.upgrades.info2"));
            lore.add("");
            lore.add(lang().get("menu.upgrades.bank", "{bank}", String.format("%,.0f", island.getBankBalance())));
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // Upgrades layout
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (Upgrade upgrade : plugin.getUpgradeManager().getUpgrades()) {
            if (idx >= slots.length) break;
            int slot = slots[idx];

            int level = plugin.getUpgradeManager().getLevel(island, upgrade.getId());
            boolean maxed = level >= upgrade.getMaxLevel();
            double cost = maxed ? 0 : upgrade.getCost(level + 1);

            ItemStack item = new ItemStack(upgrade.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String prefix = maxed ? ChatColor.GOLD + "" + ChatColor.BOLD + "★ " : ChatColor.AQUA + "" + ChatColor.BOLD + "✦ ";
                meta.setDisplayName(prefix + ChatColor.translateAlternateColorCodes('&', upgrade.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                lore.add("");
                lore.add(lang().get("menu.upgrades.level", "{level}", String.valueOf(level), "{max}", String.valueOf(upgrade.getMaxLevel())));
                lore.add(progressBar(level, upgrade.getMaxLevel()));
                lore.add("");
                if (!maxed) {
                    lore.add(lang().get("menu.upgrades.next-level"));
                    lore.add(lang().get("menu.upgrades.cost", "{cost}", String.format("%,.0f", cost)));
                    lore.add("");
                    lore.add(lang().get("menu.upgrades.click-upgrade"));
                } else {
                    lore.add(lang().get("menu.upgrades.max-level"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            upgradeIds.add(upgrade.getId());
            idx++;
        }

        // Prestige button (issue 4) — shown inside the Upgrades core
        if (plugin.getCoreConfig().isPrestigeMenuInUpgrades()) {
            buildPrestigeButton();
        }

        addBackButton();
    }

    /** Slot for the prestige button in the bottom row. */
    private static final int PRESTIGE_SLOT = 40;

    private void buildPrestigeButton() {
        me.reil.skybound.core.island.PrestigeManager pm = plugin.getPrestigeManager();
        int prestige = pm.getPrestigeLevel(island.getId());
        int affordable = pm.affordableCount(island);
        long cost = pm.getCostXp();
        int tokensPer = pm.getTokensPerPrestige();
        int minLevel = pm.getMinLevelToPrestige();

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang().get("menu.upgrades.prestige-title"));
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(lang().get("menu.upgrades.prestige-current", "{prestige}", String.valueOf(prestige)));
            lore.add(lang().get("menu.upgrades.prestige-xp", "{xp}", String.format("%,d", island.getExperience())));
            lore.add(lang().get("menu.upgrades.prestige-cost", "{cost}", String.format("%,d", cost)));
            lore.add(lang().get("menu.upgrades.prestige-reward", "{tokens}", String.valueOf(tokensPer)));
            lore.add("");
            if (island.getLevel() < minLevel) {
                lore.add(lang().get("menu.upgrades.prestige-need-level", "{level}", String.valueOf(minLevel)));
                lore.add(lang().get("menu.upgrades.prestige-current-level", "{level}", String.valueOf(island.getLevel())));
            } else if (affordable <= 0) {
                lore.add(lang().get("menu.upgrades.prestige-no-xp"));
            } else {
                lore.add(lang().get("menu.upgrades.prestige-affordable", "{count}", String.valueOf(affordable)));
                lore.add("");
                lore.add(lang().get("menu.upgrades.prestige-left"));
                lore.add(lang().get("menu.upgrades.prestige-right", "{count}", String.valueOf(affordable)));
            }
            lore.add("");
            lore.add(lang().get("menu.upgrades.prestige-owner-only-lore"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(PRESTIGE_SLOT, item);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;

        // Prestige button
        if (slot == PRESTIGE_SLOT && plugin.getCoreConfig().isPrestigeMenuInUpgrades()) {
            handlePrestigeClick(event);
            return;
        }

        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) { index = i; break; }
        }
        if (index < 0 || index >= upgradeIds.size()) return;

        if (plugin.getCoreConfig().isIslandCoreDisableUpgradeMenu()
                && plugin.getAddonRegistry().isRegistered("island-core")) {
            lang().send(player, "island-core.upgrade-via-core");
            return;
        }

        String upgradeId = upgradeIds.get(index);
        boolean ok = plugin.getUpgradeManager().purchase(player, island, upgradeId);
        lang().send(player, ok ? "upgrade.purchased" : "upgrade.cannot");
        new UpgradesMenu(player, plugin, island).withParent(parentMenu).open();
    }

    private void handlePrestigeClick(InventoryClickEvent event) {
        // Only the owner can prestige (issue 2 + 4)
        if (!island.getOwner().equals(player.getUniqueId())) {
            lang().send(player, "prestige.owner-only");
            return;
        }
        me.reil.skybound.core.island.PrestigeManager pm = plugin.getPrestigeManager();
        int affordable = pm.affordableCount(island);
        if (island.getLevel() < pm.getMinLevelToPrestige()) {
            lang().send(player, "prestige.need-level",
                    "{level}", String.valueOf(pm.getMinLevelToPrestige()),
                    "{current}", String.valueOf(island.getLevel()));
            return;
        }
        if (affordable <= 0) {
            lang().send(player, "prestige.no-xp");
            return;
        }
        // Right-click = max, otherwise x1
        int count = event.isRightClick() ? affordable : 1;
        int done = pm.prestige(player, island, count);
        if (done > 0) {
            int level = pm.getPrestigeLevel(island.getId());
            lang().send(player, "prestige.done",
                    "{done}", String.valueOf(done),
                    "{level}", String.valueOf(level),
                    "{tokens}", String.valueOf(pm.getTokensPerPrestige() * done));
        } else {
            lang().send(player, "prestige.failed");
        }
        new UpgradesMenu(player, plugin, island).withParent(parentMenu).open();
    }

    private String progressBar(int current, int total) {
        int width = 20;
        double pct = total <= 0 ? 1.0 : Math.min(1.0, (double) current / total);
        int filled = (int) Math.round(pct * width);
        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.DARK_GRAY).append("[").append(ChatColor.AQUA);
        for (int i = 0; i < filled; i++) sb.append("▉");
        sb.append(ChatColor.DARK_GRAY);
        for (int i = filled; i < width; i++) sb.append("▉");
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
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
}
