package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandReviewManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Display reviews for an island. Click 1..5 nether stars at the bottom to leave/update your rating.
 */
public final class IslandReviewsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd.MM HH:mm");

    public IslandReviewsMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override public String getTitle() {
        return lang().get("menu.reviews.title", "{island}", island.getName());
    }

    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        IslandReviewManager rm = plugin.getIslandReviewManager();
        List<IslandReviewManager.Review> list = rm.getReviews(island.getId());

        // Header (slot 4)
        ItemStack header = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta hm = header.getItemMeta();
        if (hm != null) {
            hm.setDisplayName(lang().get("menu.reviews.header"));
            List<String> lore = new ArrayList<String>();
            double avg = rm.getAverageRating(island.getId());
            int count = rm.getReviewCount(island.getId());
            lore.add(lang().get("menu.reviews.average", "{rating}", String.format("%.2f", avg)));
            lore.add(lang().get("menu.reviews.count", "{count}", String.valueOf(count)));
            hm.setLore(lore);
            header.setItemMeta(hm);
        }
        inventory.setItem(4, header);

        // Reviews 9..44 (4 rows of 9)
        int slot = 9;
        for (IslandReviewManager.Review r : list) {
            if (slot >= 45) break;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + r.authorName + ChatColor.GRAY + " — " + ChatColor.GOLD + stars(r.stars));
                List<String> lore = new ArrayList<String>();
                if (!r.comment.isEmpty()) {
                    // Wrap comment at ~32 chars per line
                    String c = r.comment;
                    while (c.length() > 32) {
                        lore.add(lang().get("menu.reviews.comment-line", "{comment}", c.substring(0, 32)));
                        c = c.substring(32);
                    }
                    if (!c.isEmpty()) lore.add(lang().get("menu.reviews.comment-line", "{comment}", c));
                }
                lore.add(ChatColor.DARK_GRAY + DATE_FMT.format(new Date(r.timestamp)));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        // Star rating buttons 46..50 (5 stars)
        boolean canRate = !island.getMembers().contains(player.getUniqueId());
        if (canRate) {
            for (int s = 1; s <= 5; s++) {
                ItemStack star = new ItemStack(Material.NETHER_STAR, s);
                ItemMeta sm = star.getItemMeta();
                if (sm != null) {
                    sm.setDisplayName(ChatColor.GOLD + stars(s));
                    sm.setLore(java.util.Collections.singletonList(lang().get("menu.reviews.rate")));
                    star.setItemMeta(sm);
                }
                inventory.setItem(45 + s, star);
            }
        }

        addBackButton(53);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (slot >= 46 && slot <= 50) {
            int stars = slot - 45;
            plugin.getIslandReviewManager().submitReview(
                    island.getId(), player.getUniqueId(), player.getName(), stars, "");
            lang().send(player, "review.left", "{stars}", String.valueOf(stars));
            new IslandReviewsMenu(player, plugin, island).open();
            return;
        }
        if (slot == 53) {
            new IslandMainMenu(player, plugin, island).open();
        }
    }

    private static String stars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append('★');
        for (int i = n; i < 5; i++) sb.append('☆');
        return sb.toString();
    }
}
