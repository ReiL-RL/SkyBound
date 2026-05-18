package me.reil.skybound.core.menu;

import me.reil.skybound.api.leaderboard.LeaderboardEntry;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
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

    @Override public String getTitle() { return lang().get("menu.top.title"); }
    @Override public int getSize() { return 27; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();
        List<LeaderboardEntry> top = plugin.getLeaderboardManager().getTopByLevel(10);
        int slot = 0;
        for (LeaderboardEntry entry : top) {
            if (slot >= 10) break;
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(l.get("menu.top.rank", "{rank}", String.valueOf(entry.getRank()), "{name}", entry.getIslandName()));
                List<String> lore = new ArrayList<String>();
                lore.add(l.get("menu.top.owner", "{owner}", entry.getOwnerName()));
                lore.add(l.get("menu.top.level", "{level}", String.valueOf(entry.getLevel())));
                lore.add(l.get("menu.top.members", "{count}", String.valueOf(entry.getMemberCount())));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        // View-only
    }
}
