package me.reil.skybound.core.storage;

import me.reil.skybound.core.booster.ActiveBoosterImpl;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads active boosters to/from YAML.
 */
public final class BoosterDataStore {

    private final JavaPlugin plugin;
    private final File file;

    public BoosterDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/boosters.yml");
    }

    public Map<String, List<ActiveBoosterImpl>> load() {
        Map<String, List<ActiveBoosterImpl>> result = new LinkedHashMap<String, List<ActiveBoosterImpl>>();
        if (!file.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection islands = cfg.getConfigurationSection("islands");
        if (islands == null) return result;

        long now = System.currentTimeMillis();
        for (String islandId : islands.getKeys(false)) {
            ConfigurationSection islandSection = islands.getConfigurationSection(islandId);
            if (islandSection == null) continue;

            List<ActiveBoosterImpl> boosters = new ArrayList<ActiveBoosterImpl>();
            for (String key : islandSection.getKeys(false)) {
                ConfigurationSection bs = islandSection.getConfigurationSection(key);
                if (bs == null) continue;

                String boosterId = bs.getString("booster-id", key);
                long activatedAt = bs.getLong("activated-at", 0L);
                long expiresAt = bs.getLong("expires-at", 0L);
                double multiplier = bs.getDouble("multiplier", 1.0);

                // Only load if not expired
                if (expiresAt > now) {
                    boosters.add(new ActiveBoosterImpl(boosterId, activatedAt, expiresAt, multiplier));
                }
            }

            if (!boosters.isEmpty()) {
                result.put(islandId, boosters);
            }
        }

        return result;
    }

    public void save(Map<String, List<ActiveBoosterImpl>> data) {
        YamlConfiguration cfg = new YamlConfiguration();
        long now = System.currentTimeMillis();

        for (Map.Entry<String, List<ActiveBoosterImpl>> entry : data.entrySet()) {
            int idx = 0;
            for (ActiveBoosterImpl booster : entry.getValue()) {
                if (booster.getExpiresAt() <= now) continue; // skip expired
                String path = "islands." + entry.getKey() + "." + idx;
                cfg.set(path + ".booster-id", booster.getBoosterId());
                cfg.set(path + ".activated-at", booster.getActivatedAt());
                cfg.set(path + ".expires-at", booster.getExpiresAt());
                cfg.set(path + ".multiplier", booster.getMultiplier());
                idx++;
            }
        }

        saveFile(cfg);
    }

    private void saveFile(YamlConfiguration cfg) {
        try {
            file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save boosters.yml: " + e.getMessage());
        }
    }
}
