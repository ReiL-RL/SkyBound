package me.reil.skybound.api.shop;

import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Server shop provider.
 * Supports buy/sell with categories and command-based items.
 */
public interface ShopProvider {

    /**
     * Get all shop categories.
     */
    Collection<ShopCategory> getCategories();

    /**
     * Get a specific category by id.
     */
    ShopCategory getCategory(String categoryId);

    /**
     * Get all items in a category.
     */
    Collection<ShopItem> getItems(String categoryId);

    /**
     * Buy an item for a player.
     * @param amount quantity to buy
     * @return true if purchase was successful
     */
    boolean buy(Player player, String itemId, int amount);

    /**
     * Sell an item from a player's inventory.
     * @param amount quantity to sell
     * @return true if sale was successful
     */
    boolean sell(Player player, String itemId, int amount);

    /**
     * Get the buy price of an item.
     */
    double getBuyPrice(String itemId);

    /**
     * Get the sell price of an item.
     */
    double getSellPrice(String itemId);
}
