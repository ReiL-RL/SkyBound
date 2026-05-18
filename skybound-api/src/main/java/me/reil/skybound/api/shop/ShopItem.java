package me.reil.skybound.api.shop;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents an item in the shop.
 */
public interface ShopItem {

    /** Unique item id. */
    String getId();

    /** Display name. */
    String getDisplayName();

    /** Material. */
    Material getMaterial();

    /** Buy price (0 = cannot buy). */
    double getBuyPrice();

    /** Sell price (0 = cannot sell). */
    double getSellPrice();

    /** Default buy amount. */
    int getDefaultAmount();

    /** Slot in the category GUI. */
    int getSlot();

    /** Commands to execute on purchase (optional). */
    List<String> getCommands();

    /** Lore lines for GUI. */
    List<String> getLore();
}
