package me.reil.skybound.core.menu;

import me.reil.skybound.core.SkyBoundPlugin;
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

    public MissionCategoryMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override public String getTitle() { return lang().get("menu.missions-cat.title"); }
    @Override public int getSize() { return 27; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        categories.addAll(plugin.getMissionManager().getCategories());

        int slot = 10;
        for (String category : categories) {
            if (slot > 16) break;
            Material icon = getCategoryIcon(category);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color("&6" + capitalize(category)));
                List<String> lore = new ArrayList<String>();
                int count = plugin.getMissionManager().getMissionsByCategory(category).size();
                lore.add(color("&7" + count + " \u043c\u0438\u0441\u0441\u0438\u0439"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot, item);
            slot++;
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int index = event.getSlot() - 10;
        if (index < 0 || index >= categories.size()) return;
        new MissionsListMenu(player, plugin, categories.get(index)).open();
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
            default: return Material.PAPER;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
