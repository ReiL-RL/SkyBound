package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Island prestige system.
 * Players can reset their island in exchange for a permanent multiplier bonus.
 * Each prestige level increases rewards (money, XP) by a configurable percentage.
 */
public final class PrestigeManager {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    // islandId -> prestige level
    private final Map<String, Integer> prestigeLevels = new LinkedHashMap<String, Integer>();

    // Configurable
    private final int minLevelToPrestige;
    private final double multiplierPerPrestige;
    private final int maxPrestige;

    public PrestigeManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.minLevelToPrestige = 30; // Must be level 30+ to prestige
        this.multiplierPerPrestige = 0.1; // +10% per prestige
        this.maxPrestige = 10;
    }

    /**
     * Get the prestige level of an island.
     */
    public int getPrestigeLevel(String islandId) {
        Integer level = prestigeLevels.get(islandId);
        return level == null ? 0 : level;
    }

    /**
     * Get the multiplier bonus for an island (1.0 = no bonus, 1.1 = +10%, etc).
     */
    public double getMultiplier(String islandId) {
        return 1.0 + (getPrestigeLevel(islandId) * multiplierPerPrestige);
    }

    /**
     * Check if an island can prestige.
     */
    public boolean canPrestige(Island island) {
        if (island == null) return false;
        if (island.getLevel() < minLevelToPrestige) return false;
        return getPrestigeLevel(island.getId()) < maxPrestige;
    }

    /**
     * Perform prestige: reset island level/XP, increment prestige.
     * @return true if successful
     */
    public boolean prestige(Player player, Island island) {
        if (!canPrestige(island)) return false;

        int currentPrestige = getPrestigeLevel(island.getId());
        int newPrestige = currentPrestige + 1;

        // Reset island progress
        island.setLevel(1);
        // Reset XP by setting experience to 0 (need to handle in IslandImpl)
        island.setBankBalance(0.0);

        // Increment prestige
        prestigeLevels.put(island.getId(), newPrestige);

        plugin.getLogger().info("Island " + island.getId() + " prestiged to level " + newPrestige);
        return true;
    }

    /**
     * Get minimum level required to prestige.
     */
    public int getMinLevelToPrestige() {
        return minLevelToPrestige;
    }

    /**
     * Get max prestige level.
     */
    public int getMaxPrestige() {
        return maxPrestige;
    }

    /**
     * Get all prestige data for persistence.
     */
    public Map<String, Integer> getAllPrestigeLevels() {
        return prestigeLevels;
    }

    /**
     * Load prestige data from persistence.
     */
    public void setAllPrestigeLevels(Map<String, Integer> data) {
        prestigeLevels.clear();
        prestigeLevels.putAll(data);
    }
}
