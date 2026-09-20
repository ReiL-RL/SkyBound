package me.reil.skybound.core.menu;

import me.reil.skybound.api.shop.ShopItem;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import me.reil.skybound.core.shop.ShopItemImpl;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

    @Override public String getTitle() {
        return ChatColor.GREEN + "" + ChatColor.BOLD + "✦ " + capitalize(categoryId);
    }
    @Override public int getSize() { return 54; }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());
        LangManager l = lang();
        Collection<ShopItem> shopItems = plugin.getShopManager().getItems(categoryId);
        items.clear();
        items.addAll(shopItems);

        // Bottom decoration only — items use slots 0-44 from config
        ItemStack bottomGlass = decoration(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inventory.setItem(i, bottomGlass);

        for (ShopItem shopItem : items) {
            int displayAmount = Math.max(1, Math.min(64, shopItem.getDefaultAmount()));
            ItemStack item = shopItem instanceof ShopItemImpl
                    ? ((ShopItemImpl) shopItem).createStack(displayAmount)
                    : new ItemStack(shopItem.getMaterial(), displayAmount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(shopItem.getDisplayName()));
                List<String> lore = new ArrayList<String>();

                if (shopItem.getLore() != null && !shopItem.getLore().isEmpty()) {
                    for (String line : shopItem.getLore()) {
                        lore.add(color(line));
                    }
                    lore.add("");
                }

                if (shopItem.getBuyPrice() > 0) {
                    lore.add(l.get("menu.shop.buy-price", "{price}", String.format("%,.1f", shopItem.getBuyPrice())));
                }
                if (shopItem.getSellPrice() > 0) {
                    lore.add(l.get("menu.shop.sell-price", "{price}", String.format("%,.1f", shopItem.getSellPrice())));
                }
                if (shopItem.getDefaultAmount() > 1) {
                    lore.add(l.get("menu.shop.click-amount", "{amount}", String.valueOf(shopItem.getDefaultAmount())));
                }
                lore.add("");

                if (shopItem.getBuyPrice() > 0) {
                    lore.add(l.get("menu.shop.left-buy"));
                }
                if (shopItem.getSellPrice() > 0) {
                    lore.add(l.get("menu.shop.right-sell"));
                }
                if (shopItem.getBuyPrice() > 0 || shopItem.getSellPrice() > 0) {
                    lore.add(l.get("menu.shop.shift-bulk"));
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(shopItem.getSlot(), item);
        }

        addBackButton();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        if (handleBack(slot)) return;
        ShopItem clicked = null;
        for (ShopItem item : items) {
            if (item.getSlot() == slot) { clicked = item; break; }
        }
        if (clicked == null) return;

        LangManager l = lang();
        int multiplier = event.isShiftClick() ? 10 : 1;
        int amount = Math.max(1, clicked.getDefaultAmount() * multiplier);

        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
            me.reil.skybound.core.shop.ShopManager.TransactionResult result =
                    plugin.getShopManager().buyDetailed(player, clicked.getId(), amount);
            l.send(player, shopMessageKey(result, true),
                    "{amount}", String.valueOf(amount), "{item}", clicked.getDisplayName());
        } else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
            me.reil.skybound.core.shop.ShopManager.TransactionResult result =
                    plugin.getShopManager().sellDetailed(player, clicked.getId(), amount);
            l.send(player, shopMessageKey(result, false),
                    "{amount}", String.valueOf(amount), "{item}", clicked.getDisplayName());
        }
    }

    private String shopMessageKey(me.reil.skybound.core.shop.ShopManager.TransactionResult result, boolean buying) {
        switch (result) {
            case SUCCESS: return buying ? "shop.purchased" : "shop.sold";
            case NO_MONEY: return "shop.no-money";
            case NO_SPACE: return "shop.no-space";
            case NO_ITEMS: return "shop.no-items";
            default: return "shop.unavailable";
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

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
