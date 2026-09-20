package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.PrestigeShopManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class PrestigeShopMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final List<PrestigeShopManager.PrestigeItem> items = new ArrayList<PrestigeShopManager.PrestigeItem>();
    private final int page;

    /** Inner content slots (4 rows × 7 = 28 per page). */
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;

    public PrestigeShopMenu(Player player, SkyBoundPlugin plugin) {
        this(player, plugin, 0);
    }

    public PrestigeShopMenu(Player player, SkyBoundPlugin plugin, int page) {
        super(player);
        this.plugin = plugin;
        this.page = Math.max(0, page);
    }

    @Override public String getTitle() {
        return lang().get("menu.prestige-shop.title");
    }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        items.clear();
        items.addAll(plugin.getPrestigeShopManager().getItems().values());

        // Borders
        ItemStack topGlass = decoration(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        Island island = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
        int tokens = island != null ? plugin.getPrestigeShopManager().getTokens(island.getId()) : 0;
        int prestige = island != null ? plugin.getPrestigeManager().getPrestigeLevel(island.getId()) : 0;

        // Header (slot 4)
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(lang().get("menu.prestige-shop.header"));
            List<String> iLore = new ArrayList<String>();
            iLore.add("");
            iLore.add(lang().get("menu.prestige-shop.prestige-level", "{level}", String.valueOf(prestige)));
            iLore.add(lang().get("menu.prestige-shop.tokens", "{tokens}", String.valueOf(tokens)));
            iLore.add("");
            iLore.add(lang().get("menu.prestige-shop.info1"));
            iLore.add(lang().get("menu.prestige-shop.info2"));
            iMeta.setLore(iLore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // Items in inner area (slots 10-16, 19-25, 28-34, 37-43)
        int[] slots = CONTENT_SLOTS;
        int perPage = slots.length;
        int totalPages = Math.max(1, (items.size() + perPage - 1) / perPage);
        int curPage = Math.min(page, totalPages - 1);
        int start = curPage * perPage;

        for (int i = 0; i < perPage && (start + i) < items.size(); i++) {
            int slot = slots[i];
            PrestigeShopManager.PrestigeItem pi = items.get(start + i);
            boolean canAfford = tokens >= pi.cost;

            ItemStack disp = new ItemStack(pi.material);
            ItemMeta meta = disp.getItemMeta();
            if (meta != null) {
                String prefix = canAfford ? ChatColor.GREEN + "" + ChatColor.BOLD + "✦ " : ChatColor.GRAY + "" + ChatColor.BOLD + "✘ ";
                meta.setDisplayName(prefix + ChatColor.translateAlternateColorCodes('&', pi.displayName));

                List<String> lore = new ArrayList<String>();
                for (String line : pi.lore) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                lore.add("");
                lore.add(lang().get(canAfford ? "menu.prestige-shop.cost-afford" : "menu.prestige-shop.cost-no-afford",
                        "{cost}", String.valueOf(pi.cost)));
                lore.add(lang().get("menu.prestige-shop.you-have", "{tokens}", String.valueOf(tokens)));
                lore.add("");
                if (canAfford) {
                    lore.add(lang().get("menu.prestige-shop.click-buy"));
                } else {
                    lore.add(lang().get("menu.prestige-shop.not-enough"));
                    lore.add(lang().get("menu.prestige-shop.missing", "{amount}", String.valueOf(pi.cost - tokens)));
                }
                meta.setLore(lore);
                disp.setItemMeta(meta);
            }
            inventory.setItem(slot, disp);
        }

        // Pagination buttons
        if (curPage > 0) {
            inventory.setItem(PREV_SLOT, makeNavButton(Material.ARROW,
                    lang().get("menu.page.previous"),
                    lang().get("menu.page.number", "{page}", String.valueOf(curPage), "{pages}", String.valueOf(totalPages))));
        }
        if (curPage < totalPages - 1) {
            inventory.setItem(NEXT_SLOT, makeNavButton(Material.ARROW,
                    lang().get("menu.page.next"),
                    lang().get("menu.page.number", "{page}", String.valueOf(curPage + 2), "{pages}", String.valueOf(totalPages))));
        }

        addBackButton();
    }

    private ItemStack makeNavButton(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> l = new ArrayList<String>();
            l.add(loreLine);
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;

        int perPage = CONTENT_SLOTS.length;
        int totalPages = Math.max(1, (items.size() + perPage - 1) / perPage);
        int curPage = Math.min(page, totalPages - 1);

        // Pagination navigation
        if (slot == PREV_SLOT && curPage > 0) {
            new PrestigeShopMenu(player, plugin, curPage - 1).withParent(parentMenu).open();
            return;
        }
        if (slot == NEXT_SLOT && curPage < totalPages - 1) {
            new PrestigeShopMenu(player, plugin, curPage + 1).withParent(parentMenu).open();
            return;
        }

        // Map slot -> item index (accounting for current page)
        int idx = -1;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            if (CONTENT_SLOTS[i] == slot) { idx = curPage * perPage + i; break; }
        }
        if (idx < 0 || idx >= items.size()) return;

        PrestigeShopManager.PrestigeItem pi = items.get(idx);
        Island island = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
        if (island == null) {
            lang().send(player, "island.no-island");
            return;
        }
        if (!plugin.getPrestigeShopManager().spendTokens(island.getId(), pi.cost)) {
            lang().send(player, "prestige-shop.no-tokens");
            return;
        }
        plugin.getPrestigeShopManager().grant(player, pi);
        lang().send(player, "prestige-shop.bought", "{item}", ChatColor.translateAlternateColorCodes('&', pi.displayName));
        new PrestigeShopMenu(player, plugin, curPage).withParent(parentMenu).open();
    }

    // (item building & overflow handling lives in PrestigeShopManager.grant)

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
