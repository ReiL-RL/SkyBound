package me.reil.skybound.core.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Saves and loads island upgrade levels to/from YAML.
 */
public final class UpgradeDataStore {

    private final JavaPlugin plugin;
    private final File file;

    public UpgradeDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/upgrades.yml");
    }

    public Map<String, Map<String, Integer>> load() {
        Map<String, Map<String, Integer>> result = new LinkedHashMap<String, Map<String, Integer>>();
        if (!file.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection islands = cfg.getConfigurationSection("islands");
        if (islands == null) return result;

        for (String islandId : islands.getKeys(false)) {
            ConfigurationSection islandSection = islands.getConfigurationSection(islandId);
            if (islandSection == null) continue;

            Map<String, Integer> upgrades = new LinkedHashMap<String, Integer>();
            for (String upgradeId : islandSection.getKeys(false)) {
                upgrades.put(upgradeId, islandSection.getInt(upgradeId, 0));
            }
            result.put(islandId, upgrades);
        }

        return result;
    }

    public void save(Map<String, Map<String, Integer>> data) {
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<String, Map<String, Integer>> entry : data.entrySet()) {
            for (Map.Entry<String, Integer> upgrade : entry.getValue().entrySet()) {
                cfg.set("islands." + entry.getKey() + "." + upgrade.getKey(), upgrade.getValue());
            }
        }

        saveFile(cfg);
    }

    private void saveFile(YamlConfiguration cfg) {
        try {
            file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save upgrades.yml: " + e.getMessage());
        }
    }
}
