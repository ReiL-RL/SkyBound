package me.reil.skybound.core.generator;

import me.reil.skybound.api.generator.GeneratorTier;
import org.bukkit.Material;

import java.util.Collections;
import java.util.Map;

/**
 * Default implementation of GeneratorTier.
 */
public final class GeneratorTierImpl implements GeneratorTier {

    private final String id;
    private final String displayName;
    private final int requiredLevel;
    private final Map<Material, Double> distribution;
    private final Material icon;

    public GeneratorTierImpl(String id, String displayName, int requiredLevel, Map<Material, Double> distribution, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.requiredLevel = requiredLevel;
        this.distribution = distribution;
        this.icon = icon;
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public int getRequiredLevel() { return requiredLevel; }
    @Override public Map<Material, Double> getDistribution() { return Collections.unmodifiableMap(distribution); }
    @Override public Material getIcon() { return icon; }
}
