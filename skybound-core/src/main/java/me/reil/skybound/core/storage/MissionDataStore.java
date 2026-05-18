package me.reil.skybound.core.storage;

import me.reil.skybound.core.mission.MissionProgressImpl;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saves and loads mission progress to/from YAML.
 */
public final class MissionDataStore {

    private final JavaPlugin plugin;
    private final File file;

    public MissionDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/missions-progress.yml");
    }

    public Map<UUID, Map<String, MissionProgressImpl>> load() {
        Map<UUID, Map<String, MissionProgressImpl>> result = new LinkedHashMap<UUID, Map<String, MissionProgressImpl>>();
        if (!file.exists()) return result;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) return result;

        for (String uuidStr : players.getKeys(false)) {
            UUID playerId = UUID.fromString(uuidStr);
            Map<String, MissionProgressImpl> missions = new LinkedHashMap<String, MissionProgressImpl>();

            ConfigurationSection playerSection = players.getConfigurationSection(uuidStr);
            if (playerSection == null) continue;

            for (String missionId : playerSection.getKeys(false)) {
                ConfigurationSection ms = playerSection.getConfigurationSection(missionId);
                if (ms == null) continue;

                boolean completed = ms.getBoolean("completed", false);
                boolean claimed = ms.getBoolean("claimed", false);
                long completedAt = ms.getLong("completed-at", 0L);
                long startedAt = ms.getLong("started-at", 0L);
                long lastCompletedAt = ms.getLong("last-completed-at", 0L);

                Map<String, Integer> condProgress = new LinkedHashMap<String, Integer>();
                ConfigurationSection condSection = ms.getConfigurationSection("conditions");
                if (condSection != null) {
                    for (String condId : condSection.getKeys(false)) {
                        condProgress.put(condId, condSection.getInt(condId, 0));
                    }
                }

                MissionProgressImpl progress = new MissionProgressImpl(playerId, missionId, condProgress,
                        completed, claimed, completedAt, startedAt, lastCompletedAt);
                missions.put(missionId, progress);
            }

            result.put(playerId, missions);
        }

        return result;
    }

    public void save(Map<UUID, Map<String, MissionProgressImpl>> data) {
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, MissionProgressImpl>> playerEntry : data.entrySet()) {
            String playerPath = "players." + playerEntry.getKey().toString();

            for (Map.Entry<String, MissionProgressImpl> missionEntry : playerEntry.getValue().entrySet()) {
                MissionProgressImpl progress = missionEntry.getValue();
                String path = playerPath + "." + missionEntry.getKey();

                cfg.set(path + ".completed", progress.isCompleted());
                cfg.set(path + ".claimed", progress.isClaimed());
                cfg.set(path + ".completed-at", progress.getCompletedAt());
                cfg.set(path + ".started-at", progress.getStartedAt());
                cfg.set(path + ".last-completed-at", progress.getLastCompletedAt());

                Map<String, Integer> condProgress = progress.getConditionProgress();
                for (Map.Entry<String, Integer> cond : condProgress.entrySet()) {
                    cfg.set(path + ".conditions." + cond.getKey(), cond.getValue());
                }
            }
        }

        saveFile(cfg);
    }

    private void saveFile(YamlConfiguration cfg) {
        try {
            file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save missions-progress.yml: " + e.getMessage());
        }
    }
}
