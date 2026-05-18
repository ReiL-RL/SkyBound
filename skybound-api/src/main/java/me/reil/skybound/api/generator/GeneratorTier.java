package me.reil.skybound.api.generator;

import org.bukkit.Material;

import java.util.Map;

/**
 * Represents a generator tier with its ore distribution.
 */
public interface GeneratorTier {

    /** Unique tier id. */
    String getId();

    /** Display name. */
    String getDisplayName();

    /** Required island level to unlock this tier. */
    int getRequiredLevel();

    /** Ore distribution: Material -> weight. */
    Map<Material, Double> getDistribution();

    /** Icon material for GUI. */
    Material getIcon();
}
