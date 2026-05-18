package me.reil.skybound.core.booster;

import me.reil.skybound.api.booster.Booster;
import me.reil.skybound.api.booster.BoosterType;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public final class BoosterImpl implements Booster {

    private final String id;
    private final String displayName;
    private final String description;
    private final int durationSeconds;
    private final double cost;
    private final double multiplier;
    private final BoosterType type;
    private final Material icon;

    public BoosterImpl(String id, String displayName, String description, int durationSeconds, double cost, double multiplier, BoosterType type, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.durationSeconds = durationSeconds;
        this.cost = cost;
        this.multiplier = multiplier;
        this.type = type;
        this.icon = icon;
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public List<String> getDescription() { return Arrays.asList(description); }
    @Override public int getDurationSeconds() { return durationSeconds; }
    @Override public double getCost() { return cost; }
    @Override public double getMultiplier() { return multiplier; }
    @Override public BoosterType getType() { return type; }
    @Override public Material getIcon() { return icon; }
}
