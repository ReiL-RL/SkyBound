package me.reil.skybound.api.booster;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a booster definition.
 * Boosters are temporary island-wide buffs.
 */
public interface Booster {

    /** Unique booster id. */
    String getId();

    /** Display name. */
    String getDisplayName();

    /** Description lines. */
    List<String> getDescription();

    /** Duration in seconds. */
    int getDurationSeconds();

    /** Cost to purchase. */
    double getCost();

    /** Multiplier value (e.g., 2.0 for double). */
    double getMultiplier();

    /** Booster type. */
    BoosterType getType();

    /** Icon material for GUI. */
    Material getIcon();
}
