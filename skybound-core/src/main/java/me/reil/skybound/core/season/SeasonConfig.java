package me.reil.skybound.core.season;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the seasons system.
 */
public final class SeasonConfig {

    private final JavaPlugin plugin;
    private boolean enabled;
    private int durationDays;
    private boolean autoResetIslands;
    private boolean announceEnd;
    private final Map<Integer, List<String>> rewards = new LinkedHashMap<Integer, List<String>>();

    public SeasonConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "seasons.yml");
        if (!file.exists()) {
            plugin.saveResource("seasons.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("seasons");
        if (section == null) {
            this.enabled = false;
            return;
        }

        this.enabled = section.getBoolean("enabled", true);
        this.durationDays = section.getInt("duration-days", 30);
        this.autoResetIslands = section.getBoolean("auto-reset-islands", false);
        this.announceEnd = section.getBoolean("announce-end", true);

        rewards.clear();
        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                try {
                    int rank = Integer.parseInt(key);
                    List<String> commands = rewardsSection.getStringList(key);
                    rewards.put(rank, commands);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public int getDurationDays() { return durationDays; }
    public boolean isAutoResetIslands() { return autoResetIslands; }
    public boolean isAnnounceEnd() { return announceEnd; }
    public Map<Integer, List<String>> getRewards() { return Collections.unmodifiableMap(rewards); }
}
