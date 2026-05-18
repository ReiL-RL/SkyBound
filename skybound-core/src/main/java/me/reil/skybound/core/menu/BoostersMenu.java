package me.reil.skybound.core.menu;

import me.reil.skybound.api.booster.Booster;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class BoostersMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final List<String> boosterIds = new ArrayList<String>();

    public BoostersMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override public String getTitle() { return lang().get("menu.boosters.title"); }
    @Override public int getSize() { return 27; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();
        int slot = 10;
        for (Booster booster : plugin.getBoosterManager().getBoosters()) {
            if (slot > 16) break;
            boolean active = plugin.getBoosterManager().isActive(island, booster.getId());
            long remaining = plugin.getBoosterManager().getRemainingSeconds(island, booster.getId());

            ItemStack item = new ItemStack(booster.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(booster.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                lore.add(color(booster.getDescription().get(0)));
                lore.add(l.get("booster.duration", "{minutes}", String.valueOf(booster.getDurationSeconds() / 60)));
                lore.add(l.get("booster.cost", "{cost}", String.format("%.0f", booster.getCost())));
                if (active) {
                    lore.add("");
                    lore.add(l.get("booster.active", "{time}", String.valueOf(remaining)));
                } else {
                    lore.add("");
                    lore.add(l.get("booster.click-buy"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            boosterIds.add(booster.getId());
            slot++;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int index = event.getSlot() - 10;
        if (index < 0 || index >= boosterIds.size()) return;

        // Check if Island Core addon handles boosters
        if (plugin.getCoreConfig().isIslandCoreDisableBoosterMenu()
                && plugin.getAddonRegistry().isRegistered("island-core")) {
            lang().send(player, "island-core.booster-via-core");
            return;
        }

        String boosterId = boosterIds.get(index);
        boolean ok = plugin.getBoosterManager().purchase(player, island, boosterId);
        lang().send(player, ok ? "booster.activated" : "booster.cannot");
        new BoostersMenu(player, plugin, island).open();
    }
}
