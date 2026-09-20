package me.reil.skybound.core.menu;

import me.reil.skybound.api.leaderboard.LeaderboardEntry;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class TopIslandsMenu extends Menu {

    private final SkyBoundPlugin plugin;

    public TopIslandsMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override public String getTitle() {
        return lang().get("menu.top.title");
    }
    @Override public int getSize() { return 45; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Borders
        ItemStack topGlass = decoration(Material.YELLOW_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 3; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i < 45; i++) inventory.setItem(i, bottomGlass);

        // Header
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(lang().get("menu.top.header"));
            List<String> iLore = new ArrayList<String>();
            iLore.add("");
            iLore.add(lang().get("menu.top.info"));
            iMeta.setLore(iLore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // Top entries (slots 10-25 — 14 slots in inner area, but we show 10)
        List<LeaderboardEntry> top = plugin.getLeaderboardManager().getTopByLevel(10);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < top.size() && i < slots.length; i++) {
            LeaderboardEntry entry = top.get(i);
            int rank = entry.getRank();

            Material icon;
            String prefix;
            switch (rank) {
                case 1: icon = Material.GOLD_BLOCK; prefix = ChatColor.GOLD + "" + ChatColor.BOLD + "★ "; break;
                case 2: icon = Material.IRON_BLOCK; prefix = ChatColor.GRAY + "" + ChatColor.BOLD + "★ "; break;
                case 3: icon = Material.NETHER_BRICKS; prefix = ChatColor.DARK_RED + "" + ChatColor.BOLD + "★ "; break;
                default: icon = Material.PLAYER_HEAD; prefix = ChatColor.YELLOW + "#"; break;
            }

            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(prefix + ChatColor.AQUA + rank + ChatColor.GRAY + " — " + ChatColor.WHITE + entry.getIslandName());
                List<String> lore = new ArrayList<String>();
                lore.add("");
                lore.add(lang().get("menu.top.owner", "{owner}", entry.getOwnerName()));
                lore.add(lang().get("menu.top.level", "{level}", String.valueOf(entry.getLevel())));
                lore.add(lang().get("menu.top.members", "{count}", String.valueOf(entry.getMemberCount())));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slots[i], item);
        }

        addBackButton();
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
