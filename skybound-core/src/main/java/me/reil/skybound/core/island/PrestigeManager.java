package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.PrestigeProvider;
import me.reil.skybound.core.config.CoreConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Island prestige system.
 * Players sacrifice island levels (XP) to earn prestige tokens for the prestige shop.
 * No max prestige cap, no multiplier — purely a token accumulator.
 */
public final class PrestigeManager implements PrestigeProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    private final File dataFile;
    // islandId -> prestige level
    private final Map<String, Integer> prestigeLevels = new LinkedHashMap<String, Integer>();

    public PrestigeManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.dataFile = new File(plugin.getDataFolder(), "data/prestige-levels.yml");
        load();
    }

    /** Minimum island level required to prestige (from config). */
    private int minLevel() {
        return config.getPrestigeMinLevel();
    }

    /** XP cost for a single prestige (from config). */
    public long getCostXp() {
        return config.getPrestigeCostXp();
    }

    /** Tokens granted per single prestige (from config). */
    public int getTokensPerPrestige() {
        return config.getPrestigeTokensPerPrestige();
    }

    /**
     * How many prestiges the island can currently afford in one go,
     * limited by both XP and the min-level requirement.
     */
    public int affordableCount(Island island) {
        if (island == null) return 0;
        if (island.getLevel() < minLevel()) return 0;
        long cost = getCostXp();
        if (cost <= 0L) return 0;
        long xp = island.getExperience();
        long n = xp / cost;
        return (int) Math.max(0L, Math.min(n, (long) Integer.MAX_VALUE));
    }

    /**
     * Optional reference to prestige shop, set by SkyBoundPlugin after construction
     * to avoid circular dependency. When set, prestige() will award tokens.
     */
    private me.reil.skybound.core.island.PrestigeShopManager prestigeShopManager;

    public void setPrestigeShopManager(me.reil.skybound.core.island.PrestigeShopManager mgr) {
        this.prestigeShopManager = mgr;
    }

    @Override
    public int getPrestigeLevel(String islandId) {
        Integer level = prestigeLevels.get(islandId);
        return level == null ? 0 : level;
    }

    @Override
    public void setPrestigeLevel(String islandId, int level) {
        if (level <= 0) {
            prestigeLevels.remove(islandId);
        } else {
            prestigeLevels.put(islandId, level);
        }
        save();
    }

    /**
     * Multiplier is no longer used. Always returns 1.0.
     * Kept for API compatibility.
     */
    @Override
    public double getMultiplier(String islandId) {
        return 1.0;
    }

    @Override
    public boolean canPrestige(Island island) {
        if (island == null) return false;
        return island.getLevel() >= minLevel() && island.getExperience() >= getCostXp();
    }

    /**
     * Perform a single prestige (delegates to bulk with count=1).
     */
    @Override
    public boolean prestige(Player player, Island island) {
        return prestige(player, island, 1) > 0;
    }

    /**
     * Perform up to {@code count} prestiges at once. Deducts {@code count × costXp}
     * from the island XP (keeping the remainder), grants tokens, increments prestige.
     * Bank is NOT touched. Returns the number of prestiges actually performed
     * (limited by available XP and min-level requirement).
     */
    public int prestige(Player player, Island island, int count) {
        if (island == null || count <= 0) return 0;
        if (island.getLevel() < minLevel()) return 0;

        long cost = getCostXp();
        if (cost <= 0L) return 0;

        int affordable = affordableCount(island);
        int doCount = Math.min(count, affordable);
        if (doCount <= 0) return 0;

        long totalCost = cost * (long) doCount;
        long currentXp = island.getExperience();
        long remaining = Math.max(0L, currentXp - totalCost);

        if (island instanceof IslandImpl) {
            ((IslandImpl) island).setExperience(remaining);
        } else {
            island.addExperience(-totalCost);
        }

        int currentPrestige = getPrestigeLevel(island.getId());
        int newPrestige = currentPrestige + doCount;
        prestigeLevels.put(island.getId(), newPrestige);

        if (prestigeShopManager != null) {
            prestigeShopManager.addTokens(island.getId(), getTokensPerPrestige() * doCount);
        }
        islandManager.saveData();
        save();

        plugin.getLogger().info("Island " + island.getId() + " prestiged +" + doCount
                + " → level " + newPrestige + " (cost=" + totalCost + " XP, remaining=" + remaining + ")");
        return doCount;
    }

    @Override
    public int getMinLevelToPrestige() {
        return minLevel();
    }

    /**
     * No upper limit anymore. Returns Integer.MAX_VALUE for API compatibility.
     */
    @Override
    public int getMaxPrestige() {
        return Integer.MAX_VALUE;
    }

    public Map<String, Integer> getAllPrestigeLevels() {
        return prestigeLevels;
    }

    public void setAllPrestigeLevels(Map<String, Integer> data) {
        prestigeLevels.clear();
        prestigeLevels.putAll(data);
        save();
    }

    public void removeIsland(String islandId) {
        if (islandId == null || islandId.isEmpty()) return;
        if (prestigeLevels.remove(islandId) != null) {
            save();
        }
    }

    private void load() {
        prestigeLevels.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("prestige");
        if (sec == null) return;
        for (String islandId : sec.getKeys(false)) {
            int level = sec.getInt(islandId, 0);
            if (level > 0) {
                prestigeLevels.put(islandId, level);
            }
        }
    }

    private void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : prestigeLevels.entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0) {
                cfg.set("prestige." + entry.getKey(), entry.getValue());
            }
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "prestige-levels.yml");
    }
}
