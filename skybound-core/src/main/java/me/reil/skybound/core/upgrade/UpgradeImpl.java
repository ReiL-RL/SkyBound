package me.reil.skybound.core.upgrade;

import me.reil.skybound.api.upgrade.Upgrade;
import me.reil.skybound.api.upgrade.UpgradeType;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public final class UpgradeImpl implements Upgrade {

    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final double[] costs;
    private final double[] values;
    private final UpgradeType type;
    private final Material icon;

    public UpgradeImpl(String id, String displayName, int maxLevel, double[] costs, double[] values, UpgradeType type, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.costs = costs;
        this.values = values;
        this.type = type;
        this.icon = icon;
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public List<String> getDescription() { return Arrays.asList("&7Level up to improve your island."); }
    @Override public int getMaxLevel() { return maxLevel; }

    @Override
    public double getCost(int level) {
        if (level < 1 || level > maxLevel) return 0.0;
        return costs[level - 1];
    }

    @Override
    public double getValue(int level) {
        if (level < 1 || level > maxLevel) return 0.0;
        return values[level - 1];
    }

    @Override public Material getIcon() { return icon; }
    @Override public UpgradeType getType() { return type; }
}
