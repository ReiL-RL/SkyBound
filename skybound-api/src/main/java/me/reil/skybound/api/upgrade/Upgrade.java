package me.reil.skybound.api.upgrade;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents an upgrade definition.
 * Upgrades are permanent island improvements with multiple levels.
 */
public interface Upgrade {

    /** Unique upgrade id. */
    String getId();

    /** Display name. */
    String getDisplayName();

    /** Description. */
    List<String> getDescription();

    /** Maximum level. */
    int getMaxLevel();

    /** Cost for a specific level. */
    double getCost(int level);

    /** Value at a specific level (e.g., size bonus, speed multiplier). */
    double getValue(int level);

    /** Icon material for GUI. */
    Material getIcon();

    /** Upgrade type. */
    UpgradeType getType();
}
