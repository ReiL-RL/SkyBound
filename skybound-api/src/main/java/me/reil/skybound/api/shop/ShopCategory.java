package me.reil.skybound.api.shop;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a shop category (e.g., Blocks, Food, Ores, Farming).
 */
public interface ShopCategory {

    /** Unique category id. */
    String getId();

    /** Display name. */
    String getDisplayName();

    /** Description lines. */
    List<String> getDescription();

    /** Icon material for GUI. */
    Material getIcon();

    /** Slot in the category selection GUI. */
    int getSlot();

    /** Items in this category. */
    List<ShopItem> getItems();
}
