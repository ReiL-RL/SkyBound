package me.reil.skybound.core.menu;

import me.reil.skybound.api.mission.MissionProgress;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.mission.MissionImpl;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MissionCategoryMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final List<String> categories = new ArrayList<String>();

    // Center 3x7 grid for category icons
    private static final int[] CATEGORY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public MissionCategoryMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override public String getTitle() { return lang().get("menu.missions-cat.title"); }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        categories.clear();
        categories.addAll(plugin.getMissionManager().getCategories());

        // Decorative borders
        ItemStack topGlass = decoration(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            inventory.setItem(i, topGlass);
        }
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        // Header with overall stats (slot 4)
        int totalMissions = 0;
        int completed = 0;
        for (String cat : categories) {
            for (MissionImpl m : plugin.getMissionManager().getMissionsByCategory(cat)) {
                totalMissions++;
                MissionProgress p = plugin.getMissionManager().getProgress(player.getUniqueId(), m.getId());
                if (p != null && p.isClaimed()) completed++;
            }
        }
        ItemStack header = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta hMeta = header.getItemMeta();
        if (hMeta != null) {
            hMeta.setDisplayName(lang().get("menu.missions-cat.header"));
            List<String> hLore = new ArrayList<String>();
            hLore.add("");
            hLore.add(lang().get("menu.missions-cat.categories", "{count}", String.valueOf(categories.size())));
            hLore.add(lang().get("menu.missions-cat.total", "{count}", String.valueOf(totalMissions)));
            hLore.add(lang().get("menu.missions-cat.completed", "{completed}", String.valueOf(completed), "{total}", String.valueOf(totalMissions)));
            hLore.add("");
            hLore.add(lang().get("menu.missions-cat.hint"));
            hMeta.setLore(hLore);
            header.setItemMeta(hMeta);
        }
        inventory.setItem(4, header);

        // Category icons
        int idx = 0;
        for (String category : categories) {
            if (idx >= CATEGORY_SLOTS.length) break;
            int slot = CATEGORY_SLOTS[idx];

            int catCount = plugin.getMissionManager().getMissionsByCategory(category).size();
            int catCompleted = 0;
            for (MissionImpl m : plugin.getMissionManager().getMissionsByCategory(category)) {
                MissionProgress p = plugin.getMissionManager().getProgress(player.getUniqueId(), m.getId());
                if (p != null && p.isClaimed()) catCompleted++;
            }

            ItemStack item = new ItemStack(getCategoryIcon(category));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + capitalize(category));
                List<String> lore = new ArrayList<String>();
                lore.add("");
                lore.add(lang().get("menu.missions-cat.missions", "{count}", String.valueOf(catCount)));
                lore.add(lang().get("menu.missions-cat.completed", "{completed}", String.valueOf(catCompleted), "{total}", String.valueOf(catCount)));
                if (catCount > 0) {
                    int pct = (int) Math.round(100.0 * catCompleted / catCount);
                    lore.add(lang().get("menu.missions-cat.progress", "{percent}", String.valueOf(pct)));
                }
                lore.add("");
                lore.add(lang().get("menu.missions-cat.open"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            idx++;
        }

        addBackButton();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (CATEGORY_SLOTS[i] == slot) {
                if (i < categories.size()) {
                    new MissionsListMenu(player, plugin, categories.get(i)).withParent(this).open();
                }
                return;
            }
        }
    }

    private ItemStack decoration(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }

    private Material getCategoryIcon(String category) {
        switch (category.toLowerCase()) {
            case "mining": return Material.IRON_PICKAXE;
            case "farming": return Material.WHEAT;
            case "combat": return Material.DIAMOND_SWORD;
            case "fishing": return Material.FISHING_ROD;
            case "crafting": return Material.CRAFTING_TABLE;
            case "building": return Material.BRICKS;
            case "economy": return Material.GOLD_INGOT;
            case "alchemy": return Material.BREWING_STAND;
            case "animals": return Material.LEAD;
            case "island": return Material.GRASS_BLOCK;
            case "generator": return Material.HOPPER;
            case "collection": return Material.CHEST;
            case "food": return Material.GOLDEN_APPLE;
            case "exploration": return Material.COMPASS;
            case "experience": return Material.EXPERIENCE_BOTTLE;
            case "daily": return Material.CLOCK;
            case "weekly": return Material.NETHER_STAR;
            case "challenge": return Material.TNT;
            case "special": return Material.BEACON;
            default: return Material.PAPER;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
