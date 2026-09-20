package me.reil.skybound.core.menu;

import me.reil.skybound.api.shop.ShopCategory;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ShopCategoryMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final List<ShopCategory> categories = new ArrayList<ShopCategory>();

    public ShopCategoryMenu(Player player, SkyBoundPlugin plugin) {
        super(player);
        this.plugin = plugin;
    }

    @Override public String getTitle() { return lang().get("menu.shop-cat.title"); }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        categories.clear();

        // Borders
        ItemStack topGlass = decoration(Material.LIME_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) if (i != 4) inventory.setItem(i, topGlass);
        ItemStack sideGlass = decoration(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, sideGlass);
            inventory.setItem(row * 9 + 8, sideGlass);
        }
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        // Header
        ItemStack info = new ItemStack(Material.EMERALD);
        ItemMeta iMeta = info.getItemMeta();
        if (iMeta != null) {
            iMeta.setDisplayName(lang().get("menu.shop-cat.header"));
            List<String> lore = new ArrayList<String>();
            lore.add("");
            lore.add(lang().get("menu.shop-cat.info"));
            lore.add(lang().get("menu.shop-cat.count", "{count}", String.valueOf(plugin.getShopManager().getCategories().size())));
            lore.add("");
            lore.add(lang().get("menu.shop-cat.open-hint"));
            iMeta.setLore(lore);
            info.setItemMeta(iMeta);
        }
        inventory.setItem(4, info);

        // Categories at their configured slots
        for (ShopCategory cat : plugin.getShopManager().getCategories()) {
            categories.add(cat);
            ItemStack item = new ItemStack(cat.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', cat.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                lore.add("");
                int count = plugin.getShopManager().getItems(cat.getId()).size();
                lore.add(lang().get("menu.shop-cat.items", "{count}", String.valueOf(count)));
                lore.add("");
                lore.add(lang().get("menu.main.click-open"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(cat.getSlot(), item);
        }

        addBackButton();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;
        for (ShopCategory cat : categories) {
            if (cat.getSlot() == slot) {
                new ShopItemsMenu(player, plugin, cat.getId()).withParent(this).open();
                return;
            }
        }
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
