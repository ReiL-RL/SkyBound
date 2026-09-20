package me.reil.skybound.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core configuration manager.
 * Loads and provides access to all core settings.
 */
public final class CoreConfig {

    private final JavaPlugin plugin;

    // Island world
    private String islandWorldName;
    private int islandSpacing;
    private int defaultRadius;
    private int baseY;
    private boolean netherEnabled;
    private int netherUnlockLevel;
    private boolean endEnabled;
    private int endUnlockLevel;

    // General
    private String language;
    private long autosaveSeconds;

    // Economy
    private String currencyName;
    private String currencyNamePlural;
    private double shopBuyMultiplier;
    private double shopSellMultiplier;
    private double missionMoneyMultiplier;
    private double missionXpMultiplier;

    // Island
    private int maxTeamSize;
    private int maxWarps;
    private double baseBankLimit;
    private int baseEntityLimit;
    private boolean islandLockDefault;

    // Generator
    private boolean generatorEnabled;

    // Storage
    private String storageMode;

    // Block values
    private Map<String, Integer> blockValues;

    // Leveling
    private int xpPerLevel;

    // XP addon control
    private boolean disablePassiveXpIfAddon;

    // Island Core addon integration
    private boolean islandCoreDisableBoosterMenu;
    private boolean islandCoreDisableUpgradeMenu;
    private boolean islandCoreDisablePassiveXp;

    // Economy type
    private String economyType;

    // Prestige
    private int prestigeMinLevel;
    private long prestigeCostXp;
    private int prestigeTokensPerPrestige;
    private boolean prestigeMenuInUpgrades;

    public CoreConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // Island world
        this.islandWorldName = config.getString("island-world.name", "skybound_islands");
        this.islandSpacing = config.getInt("island-world.spacing", 256);
        this.defaultRadius = config.getInt("island-world.default-radius", 60);
        this.baseY = config.getInt("island-world.base-y", 100);
        this.netherEnabled = config.getBoolean("island-world.nether.enabled", true);
        this.netherUnlockLevel = config.getInt("island-world.nether.unlock-level", 10);
        this.endEnabled = config.getBoolean("island-world.end.enabled", true);
        this.endUnlockLevel = config.getInt("island-world.end.unlock-level", 25);

        // General
        this.language = config.getString("language", "en");
        this.autosaveSeconds = config.getLong("autosave-seconds", 120L);

        // Economy
        this.currencyName = config.getString("economy.currency-name", "coin");
        this.currencyNamePlural = config.getString("economy.currency-name-plural", "coins");
        this.shopBuyMultiplier = sanitizePositiveMultiplier(config.getDouble("economy.shop.buy-multiplier", 1.0), 1.0);
        this.shopSellMultiplier = sanitizePositiveMultiplier(config.getDouble("economy.shop.sell-multiplier", 1.0), 1.0);
        this.missionMoneyMultiplier = sanitizePositiveMultiplier(config.getDouble("economy.missions.money-multiplier", 1.0), 1.0);
        this.missionXpMultiplier = sanitizePositiveMultiplier(config.getDouble("economy.missions.island-xp-multiplier", 1.0), 1.0);

        // Island
        this.maxTeamSize = config.getInt("island.max-team-size", 4);
        this.maxWarps = config.getInt("island.max-warps", 3);
        this.baseBankLimit = config.getDouble("island.base-bank-limit", 1000000.0);
        if (Double.isNaN(this.baseBankLimit) || Double.isInfinite(this.baseBankLimit) || this.baseBankLimit < 0.0) {
            this.baseBankLimit = 1000000.0;
        }
        this.baseEntityLimit = config.getInt("island.base-entity-limit", 50);
        if (this.baseEntityLimit < 1) this.baseEntityLimit = 50;
        this.islandLockDefault = config.getBoolean("island.lock-default", false);

        // Generator
        this.generatorEnabled = config.getBoolean("generator.enabled", true);

        // Storage
        this.storageMode = config.getString("storage.mode", "yaml");

        // Block values
        this.blockValues = new LinkedHashMap<String, Integer>();
        ConfigurationSection bvSection = config.getConfigurationSection("block-values");
        if (bvSection != null) {
            for (String key : bvSection.getKeys(false)) {
                blockValues.put(key, bvSection.getInt(key, 0));
            }
        }

        // Leveling
        this.xpPerLevel = config.getInt("island.xp-per-level", 100);

        // XP addon control
        this.disablePassiveXpIfAddon = config.getBoolean("xp.disable-passive-if-addon", true);

        // Island Core addon integration
        this.islandCoreDisableBoosterMenu = config.getBoolean("island-core.disable-booster-menu", true);
        this.islandCoreDisableUpgradeMenu = config.getBoolean("island-core.disable-upgrade-menu", true);
        this.islandCoreDisablePassiveXp = config.getBoolean("island-core.disable-passive-xp", true);

        // Economy type
        this.economyType = config.getString("economy.type", "VAULT");

        // Prestige
        this.prestigeMinLevel = config.getInt("prestige.min-level", 30);
        // Cost in XP per single prestige. If not set (<=0), default to min-level × xp-per-level.
        long defaultCost = (long) this.prestigeMinLevel * (long) this.xpPerLevel;
        this.prestigeCostXp = config.getLong("prestige.cost-xp", defaultCost);
        if (this.prestigeCostXp <= 0L) this.prestigeCostXp = defaultCost;
        this.prestigeTokensPerPrestige = config.getInt("prestige.tokens-per-prestige", 1);
        if (this.prestigeTokensPerPrestige < 1) this.prestigeTokensPerPrestige = 1;
        // Show prestige button inside the upgrades menu ("Ядро улучшений")
        this.prestigeMenuInUpgrades = config.getBoolean("prestige.show-in-upgrades", true);
    }

    public void reload() {
        load();
    }

    // Getters
    public String getIslandWorldName() { return islandWorldName; }
    public int getIslandSpacing() { return islandSpacing; }
    public int getDefaultRadius() { return defaultRadius; }
    public int getBaseY() { return baseY; }
    public boolean isNetherEnabled() { return netherEnabled; }
    public int getNetherUnlockLevel() { return netherUnlockLevel; }
    public boolean isEndEnabled() { return endEnabled; }
    public int getEndUnlockLevel() { return endUnlockLevel; }
    public String getLanguage() { return language; }
    public long getAutosaveSeconds() { return autosaveSeconds; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencyNamePlural() { return currencyNamePlural; }
    public double getShopBuyMultiplier() { return shopBuyMultiplier; }
    public double getShopSellMultiplier() { return shopSellMultiplier; }
    public double getMissionMoneyMultiplier() { return missionMoneyMultiplier; }
    public double getMissionXpMultiplier() { return missionXpMultiplier; }
    public int getMaxTeamSize() { return maxTeamSize; }
    public int getMaxWarps() { return maxWarps; }
    public double getBaseBankLimit() { return baseBankLimit; }
    public int getBaseEntityLimit() { return baseEntityLimit; }
    public boolean isIslandLockDefault() { return islandLockDefault; }
    public boolean isGeneratorEnabled() { return generatorEnabled; }
    public String getStorageMode() { return storageMode; }
    public Map<String, Integer> getBlockValues() { return blockValues; }
    public int getXpPerLevel() { return xpPerLevel; }
    public boolean isDisablePassiveXpIfAddon() { return disablePassiveXpIfAddon; }
    public boolean isIslandCoreDisableBoosterMenu() { return islandCoreDisableBoosterMenu; }
    public boolean isIslandCoreDisableUpgradeMenu() { return islandCoreDisableUpgradeMenu; }
    public boolean isIslandCoreDisablePassiveXp() { return islandCoreDisablePassiveXp; }
    public String getEconomyType() { return economyType; }

    // Prestige getters
    public int getPrestigeMinLevel() { return prestigeMinLevel; }
    public long getPrestigeCostXp() { return prestigeCostXp; }
    public int getPrestigeTokensPerPrestige() { return prestigeTokensPerPrestige; }
    public boolean isPrestigeMenuInUpgrades() { return prestigeMenuInUpgrades; }

    private double sanitizePositiveMultiplier(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return fallback;
        }
        return value;
    }
}
