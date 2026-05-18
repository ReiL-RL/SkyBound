package me.reil.skybound.core.storage;

import me.reil.skybound.core.booster.ActiveBoosterImpl;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandImpl;
import me.reil.skybound.core.mission.MissionProgressImpl;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StorageManager {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private IslandDataStore islandStore;
    private MissionDataStore missionStore;
    private UpgradeDataStore upgradeStore;
    private BoosterDataStore boosterStore;

    // References set after managers are created
    private Runnable saveCallback;

    public StorageManager(JavaPlugin plugin, CoreConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void initialize() {
        File dataDir = new File(plugin.getDataFolder(), "data");
        dataDir.mkdirs();

        this.islandStore = new IslandDataStore(plugin);
        this.missionStore = new MissionDataStore(plugin);
        this.upgradeStore = new UpgradeDataStore(plugin);
        this.boosterStore = new BoosterDataStore(plugin);

        plugin.getLogger().info("Storage initialized (mode: " + config.getStorageMode() + ")");
    }

    public void setSaveCallback(Runnable callback) {
        this.saveCallback = callback;
    }

    public void saveAll() {
        if (saveCallback != null) {
            saveCallback.run();
        }
    }

    public void close() {
        // Nothing for YAML
    }

    public Map<String, IslandImpl> loadIslands() {
        return islandStore.load();
    }

    public void saveIslands(Map<String, IslandImpl> islands) {
        islandStore.save(islands);
    }

    public Map<UUID, Map<String, MissionProgressImpl>> loadMissionProgress() {
        return missionStore.load();
    }

    public void saveMissionProgress(Map<UUID, Map<String, MissionProgressImpl>> data) {
        missionStore.save(data);
    }

    public Map<String, Map<String, Integer>> loadUpgrades() {
        return upgradeStore.load();
    }

    public void saveUpgrades(Map<String, Map<String, Integer>> data) {
        upgradeStore.save(data);
    }

    public Map<String, List<ActiveBoosterImpl>> loadBoosters() {
        return boosterStore.load();
    }

    public void saveBoosters(Map<String, List<ActiveBoosterImpl>> data) {
        boosterStore.save(data);
    }
}
