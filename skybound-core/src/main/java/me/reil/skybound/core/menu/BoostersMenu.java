package me.reil.skybound.core.menu;

import me.reil.skybound.api.booster.Booster;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    @Override public int getSize() { return 45; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        boosterIds.clear();

        // Borders
        ItemStack topGlass = decoration(Material.MAGENTA_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 3; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inventory.setItem(i, bottomGlass);

        // Header
        ItemStack info = new ItemStack(Material.BREWING_STAND);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(lang().get("menu.boosters.header"));
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(lang().get("menu.boosters.info1"));
            lore.add(lang().get("menu.boosters.info2"));
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // Boosters layout (slots 19-25)
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (Booster booster : plugin.getBoosterManager().getBoosters()) {
            if (idx >= slots.length) break;
            int slot = slots[idx];

            boolean active = plugin.getBoosterManager().isActive(island, booster.getId());
            long remaining = plugin.getBoosterManager().getRemainingSeconds(island, booster.getId());

            ItemStack item = new ItemStack(active ? Material.GLOWSTONE : booster.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String prefix = active ? ChatColor.GREEN + "" + ChatColor.BOLD + "● " : ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ ";
                meta.setDisplayName(prefix + ChatColor.translateAlternateColorCodes('&', booster.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                if (!booster.getDescription().isEmpty()) {
                    for (String line : booster.getDescription()) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    lore.add("");
                }
                lore.add(lang().get("menu.boosters.multiplier", "{multiplier}", String.valueOf(booster.getMultiplier())));
                lore.add(lang().get("menu.boosters.duration", "{minutes}", String.valueOf(booster.getDurationSeconds() / 60)));
                lore.add(lang().get("menu.boosters.cost", "{cost}", String.format("%,.0f", booster.getCost())));
                lore.add("");
                if (active) {
                    lore.add(lang().get("menu.boosters.active"));
                    lore.add(lang().get("menu.boosters.remaining", "{time}", formatTime(remaining)));
                } else {
                    lore.add(lang().get("menu.boosters.click-activate"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            boosterIds.add(booster.getId());
            idx++;
        }

        addBackButton();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) { index = i; break; }
        }
        if (index < 0 || index >= boosterIds.size()) return;

        if (plugin.getCoreConfig().isIslandCoreDisableBoosterMenu()
                && plugin.getAddonRegistry().isRegistered("island-core")) {
            lang().send(player, "island-core.booster-via-core");
            return;
        }

        String boosterId = boosterIds.get(index);
        boolean ok = plugin.getBoosterManager().purchase(player, island, boosterId);
        lang().send(player, ok ? "booster.activated" : "booster.cannot");
        new BoostersMenu(player, plugin, island).withParent(parentMenu).open();
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return lang().get("time.none");
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return lang().get("time.hours-minutes", "{hours}", String.valueOf(h), "{minutes}", String.valueOf(m));
        if (m > 0) return lang().get("time.minutes-seconds", "{minutes}", String.valueOf(m), "{seconds}", String.valueOf(s));
        return lang().get("time.seconds", "{seconds}", String.valueOf(s));
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
