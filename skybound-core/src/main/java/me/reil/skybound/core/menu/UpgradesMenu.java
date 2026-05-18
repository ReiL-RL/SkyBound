package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.upgrade.Upgrade;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
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
    @Override public int getSize() { return 27; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();
        int slot = 10;
        for (Upgrade upgrade : plugin.getUpgradeManager().getUpgrades()) {
            if (slot > 16) break;
            int level = plugin.getUpgradeManager().getLevel(island, upgrade.getId());
            boolean maxed = level >= upgrade.getMaxLevel();
            double cost = maxed ? 0 : upgrade.getCost(level + 1);

            ItemStack item = new ItemStack(upgrade.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(upgrade.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                lore.add(l.get("upgrade.level", "{current}", String.valueOf(level), "{max}", String.valueOf(upgrade.getMaxLevel())));
                if (!maxed) {
                    lore.add(l.get("upgrade.cost", "{cost}", String.format("%.0f", cost)));
                    lore.add("");
                    lore.add(l.get("upgrade.click-buy"));
                } else {
                    lore.add(l.get("upgrade.max-level"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            upgradeIds.add(upgrade.getId());
            slot++;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int index = event.getSlot() - 10;
        if (index < 0 || index >= upgradeIds.size()) return;

        // Check if Island Core addon handles generator upgrades
        if (plugin.getCoreConfig().isIslandCoreDisableUpgradeMenu()
                && plugin.getAddonRegistry().isRegistered("island-core")) {
            lang().send(player, "island-core.upgrade-via-core");
            return;
        }

        String upgradeId = upgradeIds.get(index);
        boolean ok = plugin.getUpgradeManager().purchase(player, island, upgradeId);
        lang().send(player, ok ? "upgrade.purchased" : "upgrade.cannot");
        new UpgradesMenu(player, plugin, island).open();
    }
}
