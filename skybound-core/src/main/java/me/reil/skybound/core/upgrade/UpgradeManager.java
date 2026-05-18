package me.reil.skybound.core.upgrade;

import me.reil.skybound.api.event.IslandUpgradeEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.upgrade.Upgrade;
import me.reil.skybound.api.upgrade.UpgradeProvider;
import me.reil.skybound.api.upgrade.UpgradeType;
import me.reil.skybound.core.bank.BankManager;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UpgradeManager implements UpgradeProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    private final BankManager bankManager;
    private final Map<String, UpgradeImpl> upgrades = new LinkedHashMap<String, UpgradeImpl>();
    private final Map<String, Map<String, Integer>> islandUpgrades = new LinkedHashMap<String, Map<String, Integer>>();

    public UpgradeManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager, BankManager bankManager) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.bankManager = bankManager;
        loadUpgrades();
    }

    public void reload() {
        upgrades.clear();
        loadUpgrades();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Upgrade> getUpgrades() {
        return Collections.unmodifiableCollection((Collection<? extends Upgrade>) (Collection<?>) upgrades.values());
    }

    @Override
    public Upgrade getUpgrade(String upgradeId) {
        return upgrades.get(upgradeId);
    }

    @Override
    public int getLevel(Island island, String upgradeId) {
        Map<String, Integer> levels = islandUpgrades.get(island.getId());
        if (levels == null) return 0;
        Integer level = levels.get(upgradeId);
        return level == null ? 0 : level;
    }

    @Override
    public boolean purchase(Player buyer, Island island, String upgradeId) {
        UpgradeImpl upgrade = upgrades.get(upgradeId);
        if (upgrade == null || island == null) return false;

        int currentLevel = getLevel(island, upgradeId);
        if (currentLevel >= upgrade.getMaxLevel()) return false;

        int nextLevel = currentLevel + 1;
        double cost = upgrade.getCost(nextLevel);

        if (!bankManager.withdrawInternal(island, cost)) return false;

        IslandUpgradeEvent event = new IslandUpgradeEvent(buyer, island, upgradeId, nextLevel, cost);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            island.setBankBalance(island.getBankBalance() + cost);
            return false;
        }

        Map<String, Integer> levels = islandUpgrades.get(island.getId());
        if (levels == null) {
            levels = new LinkedHashMap<String, Integer>();
            islandUpgrades.put(island.getId(), levels);
        }
        levels.put(upgradeId, nextLevel);

        applyUpgrade(island, upgradeId, nextLevel);
        return true;
    }

    @Override
    public double getNextLevelCost(Island island, String upgradeId) {
        UpgradeImpl upgrade = upgrades.get(upgradeId);
        if (upgrade == null) return 0.0;
        int nextLevel = getLevel(island, upgradeId) + 1;
        return upgrade.getCost(nextLevel);
    }

    @Override
    public boolean isMaxLevel(Island island, String upgradeId) {
        UpgradeImpl upgrade = upgrades.get(upgradeId);
        if (upgrade == null) return true;
        return getLevel(island, upgradeId) >= upgrade.getMaxLevel();
    }

    @Override
    public double getEffectiveValue(Island island, String upgradeId) {
        UpgradeImpl upgrade = upgrades.get(upgradeId);
        if (upgrade == null) return 0.0;
        int level = getLevel(island, upgradeId);
        return level == 0 ? 0.0 : upgrade.getValue(level);
    }

    public Map<String, Map<String, Integer>> getAllUpgradeLevels() {
        return islandUpgrades;
    }

    public void setAllUpgradeLevels(Map<String, Map<String, Integer>> data) {
        islandUpgrades.clear();
        islandUpgrades.putAll(data);
    }

    /**
     * Get the entity limit for an island (base + upgrade).
     */
    public int getEntityLimit(Island island) {
        double upgradeValue = getEffectiveValue(island, "entity_limit");
        return 50 + (int) upgradeValue; // base 50
    }

    /**
     * Get the team size limit for an island (base + upgrade).
     */
    public int getTeamSizeLimit(Island island) {
        double upgradeValue = getEffectiveValue(island, "team_size");
        return config.getMaxTeamSize() + (int) upgradeValue;
    }

    private void applyUpgrade(Island island, String upgradeId, int level) {
        UpgradeImpl upgrade = upgrades.get(upgradeId);
        if (upgrade == null) return;

        if (upgrade.getType() == UpgradeType.ISLAND_SIZE) {
            int bonus = (int) upgrade.getValue(level);
            island.setRadius(config.getDefaultRadius() + bonus);
        }
    }

    private void loadUpgrades() {
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists()) {
            plugin.saveResource("upgrades.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("upgrades");
        if (section == null) return;

        for (String upgradeId : section.getKeys(false)) {
            ConfigurationSection us = section.getConfigurationSection(upgradeId);
            if (us == null) continue;

            String displayName = us.getString("display-name", upgradeId);
            Material icon = Material.matchMaterial(us.getString("icon", "PAPER"));
            if (icon == null) icon = Material.PAPER;
            UpgradeType type = parseUpgradeType(us.getString("type", "CUSTOM"));

            ConfigurationSection levelsSection = us.getConfigurationSection("levels");
            if (levelsSection == null) continue;

            int maxLevel = levelsSection.getKeys(false).size();
            double[] costs = new double[maxLevel];
            double[] values = new double[maxLevel];

            int idx = 0;
            for (String levelKey : levelsSection.getKeys(false)) {
                ConfigurationSection ls = levelsSection.getConfigurationSection(levelKey);
                if (ls != null) {
                    costs[idx] = ls.getDouble("cost", 0.0);
                    values[idx] = ls.getDouble("value", 0.0);
                }
                idx++;
            }

            upgrades.put(upgradeId, new UpgradeImpl(upgradeId, displayName, maxLevel, costs, values, type, icon));
        }

        plugin.getLogger().info("Loaded " + upgrades.size() + " upgrades.");
    }

    private UpgradeType parseUpgradeType(String str) {
        try {
            return UpgradeType.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UpgradeType.CUSTOM;
        }
    }
}
