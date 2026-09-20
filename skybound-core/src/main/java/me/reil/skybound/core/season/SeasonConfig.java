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
        this.durationDays = Math.max(1, section.getInt("duration-days", 30));
        this.autoResetIslands = section.getBoolean("auto-reset-islands", false);
        this.announceEnd = section.getBoolean("announce-end", true);

        rewards.clear();
        ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String key : rewardsSection.getKeys(false)) {
                try {
                    int rank = Integer.parseInt(key);
                    if (rank < 1) continue;
                    List<String> commands = sanitizeCommands(getCommandList(rewardsSection, key));
                    if (!commands.isEmpty()) {
                        rewards.put(rank, commands);
                    }
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

    private List<String> sanitizeCommands(List<String> raw) {
        List<String> out = new ArrayList<String>();
        for (String command : raw) {
            if (command == null) continue;
            String sanitized = command.trim();
            if (sanitized.startsWith("/")) {
                sanitized = sanitized.substring(1).trim();
            }
            if (sanitized.length() > 256) {
                plugin.getLogger().warning("Ignoring overlong season reward command.");
                continue;
            }
            if (!sanitized.isEmpty()) {
                out.add(sanitized);
            }
        }
        return out;
    }

    private List<String> getCommandList(ConfigurationSection section, String key) {
        if (section.isList(key)) {
            return section.getStringList(key);
        }
        String single = section.getString(key);
        if (single == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        out.add(single);
        return out;
    }
}
