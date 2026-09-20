package me.reil.skybound.core.menu;

import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.PlayerStatsManager;
import me.reil.skybound.core.island.PlayerStatsManager.Stat;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class PlayerStatsMenu extends Menu {

    private final SkyBoundPlugin plugin;

    public PlayerStatsMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override public String getTitle() {
        return lang().get("menu.stats.title", "{player}", player.getName());
    }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        PlayerStatsManager sm = plugin.getPlayerStatsManager();

        // Borders
        ItemStack topGlass = decoration(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        // Header — player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta hMeta = head.getItemMeta();
        if (hMeta != null) {
            hMeta.setDisplayName(lang().get("menu.stats.header", "{player}", player.getName()));
            List<String> hLore = new ArrayList<String>();
            hLore.add("");
            hLore.add(lang().get("menu.stats.info1"));
            hLore.add(lang().get("menu.stats.info2"));
            hMeta.setLore(hLore);
            head.setItemMeta(hMeta);
        }
        inventory.setItem(4, head);

        // Build stats
        addStat(10, Material.DIAMOND_PICKAXE, "stats.blocks-broken", sm.get(player.getUniqueId(), Stat.BLOCKS_BROKEN));
        addStat(11, Material.DIRT, "stats.blocks-placed", sm.get(player.getUniqueId(), Stat.BLOCKS_PLACED));
        addStat(12, Material.IRON_SWORD, "stats.mobs-killed", sm.get(player.getUniqueId(), Stat.MOBS_KILLED));
        addStat(13, Material.SKELETON_SKULL, "stats.deaths", sm.get(player.getUniqueId(), Stat.DEATHS));
        addStat(14, Material.FISHING_ROD, "stats.fish-caught", sm.get(player.getUniqueId(), Stat.FISH_CAUGHT));
        addStat(15, Material.CRAFTING_TABLE, "stats.items-crafted", sm.get(player.getUniqueId(), Stat.ITEMS_CRAFTED));
        addStat(16, Material.FURNACE, "stats.items-smelted", sm.get(player.getUniqueId(), Stat.ITEMS_SMELTED));

        addStat(19, Material.WRITTEN_BOOK, "stats.missions-completed", sm.get(player.getUniqueId(), Stat.MISSIONS_COMPLETED));
        addStat(20, Material.GOLD_INGOT, "stats.shop-buys", sm.get(player.getUniqueId(), Stat.SHOP_BUYS));
        addStat(21, Material.EMERALD, "stats.shop-sells", sm.get(player.getUniqueId(), Stat.SHOP_SELLS));
        addStat(22, Material.GOLD_BLOCK, "stats.bank-deposits", sm.get(player.getUniqueId(), Stat.BANK_DEPOSITS));
        addStat(23, Material.IRON_BLOCK, "stats.bank-withdraws", sm.get(player.getUniqueId(), Stat.BANK_WITHDRAWS));
        addStat(24, Material.EXPERIENCE_BOTTLE, "stats.xp-gained", sm.get(player.getUniqueId(), Stat.XP_GAINED));
        addStat(25, Material.LEATHER_BOOTS, "stats.distance-walked", sm.get(player.getUniqueId(), Stat.DISTANCE_WALKED));

        long sec = sm.get(player.getUniqueId(), Stat.PLAYTIME_SECONDS);
        addStat(28, Material.COMPASS, "stats.island-visits", sm.get(player.getUniqueId(), Stat.ISLAND_VISITS));
        addStat(29, Material.NETHER_STAR, "stats.events-completed", sm.get(player.getUniqueId(), Stat.EVENTS_COMPLETED));
        addStat(31, Material.CLOCK, "stats.playtime", formatTime(sec));

        addBackButton();
    }

    private void addStat(int slot, Material icon, String nameKey, long value) {
        addStat(slot, icon, nameKey, String.valueOf(value));
    }

    private void addStat(int slot, Material icon, String nameKey, String value) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang().get("menu.stats.item-title", "{name}", lang().get(nameKey)));
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.stats.value", "{value}", value));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return lang().get("time.seconds", "{seconds}", "0");
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return lang().get("time.hours-minutes", "{hours}", String.valueOf(h), "{minutes}", String.valueOf(m));
        if (m > 0) return lang().get("time.minutes-seconds", "{minutes}", String.valueOf(m), "{seconds}", String.valueOf(s));
        return lang().get("time.seconds", "{seconds}", String.valueOf(s));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        handleBack(event.getSlot());
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
