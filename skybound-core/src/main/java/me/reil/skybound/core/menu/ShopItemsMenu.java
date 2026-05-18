package me.reil.skybound.core.menu;

import me.reil.skybound.api.shop.ShopItem;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ShopItemsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final String categoryId;
    private final List<ShopItem> items = new ArrayList<ShopItem>();

    public ShopItemsMenu(Player player, SkyBoundPlugin plugin, String categoryId) {
        super(player);
        this.plugin = plugin;
        this.categoryId = categoryId;
    }

    @Override public String getTitle() { return lang().get("menu.shop-items.title"); }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();
        Collection<ShopItem> shopItems = plugin.getShopManager().getItems(categoryId);
        items.addAll(shopItems);

        for (ShopItem shopItem : items) {
            ItemStack item = new ItemStack(shopItem.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(shopItem.getDisplayName()));
                List<String> lore = new ArrayList<String>();
                if (shopItem.getBuyPrice() > 0) lore.add(l.get("menu.shop.buy-price", "{price}", String.format("%.1f", shopItem.getBuyPrice())));
                if (shopItem.getSellPrice() > 0) lore.add(l.get("menu.shop.sell-price", "{price}", String.format("%.1f", shopItem.getSellPrice())));
                lore.add("");
                lore.add(l.get("menu.shop.buy-sell"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(shopItem.getSlot(), item);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        ShopItem clicked = null;
        for (ShopItem item : items) {
            if (item.getSlot() == slot) { clicked = item; break; }
        }
        if (clicked == null) return;

        LangManager l = lang();
        if (event.getClick() == ClickType.LEFT) {
            boolean ok = plugin.getShopManager().buy(player, clicked.getId(), clicked.getDefaultAmount());
            l.send(player, ok ? "shop.purchased" : "shop.no-money", "{amount}", String.valueOf(clicked.getDefaultAmount()), "{item}", clicked.getDisplayName());
        } else if (event.getClick() == ClickType.RIGHT) {
            boolean ok = plugin.getShopManager().sell(player, clicked.getId(), clicked.getDefaultAmount());
            l.send(player, ok ? "shop.sold" : "shop.no-items", "{amount}", String.valueOf(clicked.getDefaultAmount()), "{item}", clicked.getDisplayName());
        }
    }
}
