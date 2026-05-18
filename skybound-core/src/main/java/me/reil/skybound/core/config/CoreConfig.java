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

    // Island
    private int maxTeamSize;
    private int maxWarps;
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

        // Island
        this.maxTeamSize = config.getInt("island.max-team-size", 4);
        this.maxWarps = config.getInt("island.max-warps", 3);
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
    public int getMaxTeamSize() { return maxTeamSize; }
    public int getMaxWarps() { return maxWarps; }
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
}
