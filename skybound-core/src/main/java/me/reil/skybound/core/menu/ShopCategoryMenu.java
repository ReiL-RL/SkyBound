package me.reil.skybound.core.menu;

import me.reil.skybound.api.shop.ShopCategory;
import me.reil.skybound.core.SkyBoundPlugin;
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
    @Override public int getSize() { return 27; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        for (ShopCategory cat : plugin.getShopManager().getCategories()) {
            categories.add(cat);
            ItemStack item = new ItemStack(cat.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(cat.getDisplayName()));
                item.setItemMeta(meta);
            }
            inventory.setItem(cat.getSlot(), item);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        for (ShopCategory cat : categories) {
            if (cat.getSlot() == event.getSlot()) {
                new ShopItemsMenu(player, plugin, cat.getId()).open();
                return;
            }
        }
    }
}
