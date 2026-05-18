package me.reil.skybound.core.mission;

import me.reil.skybound.api.event.MissionCompleteEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.mission.ConditionMode;
import me.reil.skybound.api.mission.Mission;
import me.reil.skybound.api.mission.MissionProgress;
import me.reil.skybound.api.mission.MissionProvider;
import me.reil.skybound.api.mission.MissionType;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.economy.VaultEconomyProvider;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Flexible mission system with multi-condition support.
 * Loads missions from missions.yml config.
 * Supports AND/OR conditions, prerequisites, cooldowns, time limits, repeatable missions.
 */
public final class MissionManager implements MissionProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    private final VaultEconomyProvider economy;
    private final Map<String, MissionImpl> missions = new LinkedHashMap<String, MissionImpl>();
    private final Map<UUID, Map<String, MissionProgressImpl>> playerProgress = new LinkedHashMap<UUID, Map<String, MissionProgressImpl>>();

    public MissionManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager, VaultEconomyProvider economy) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.economy = economy;
        loadMissions();
    }

    public void reload() {
        missions.clear();
        loadMissions();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Mission> getMissions() {
        return Collections.unmodifiableCollection((Collection<? extends Mission>) (Collection<?>) missions.values());
    }

    @Override
    public Mission getMission(String missionId) {
        return missions.get(missionId);
    }

    @Override
    public MissionProgress getProgress(UUID playerId, String missionId) {
        Map<String, MissionProgressImpl> map = playerProgress.get(playerId);
        if (map == null) return null;
        return map.get(missionId);
    }

    @Override
    public void addProgress(UUID playerId, String missionId, int amount) {
        // This method is kept for API compatibility but the real tracking
        // goes through trackAction()
    }

    /**
     * Track a player action and update all matching mission conditions.
     */
    public void trackAction(UUID playerId, MissionType actionType, String actionTarget, int amount) {
        Island island = islandManager.getPlayerIsland(playerId);
        int islandLevel = island != null ? island.getLevel() : 0;

        for (MissionImpl mission : missions.values()) {
            // Check level requirement
            if (mission.getRequiredLevel() > islandLevel) continue;

            // Check prerequisites
            if (!arePrerequisitesMet(playerId, mission)) continue;

            // Get or create progress
            MissionProgressImpl progress = getOrCreateProgress(playerId, mission.getId());

            // Check daily/weekly reset
            if (mission.getResetType() != null) {
                if (shouldResetMission(progress, mission.getResetType())) {
                    progress.reset();
                }
            }

            // Skip if completed and not repeatable
            if (progress.isCompleted()) {
                if (!mission.isRepeatable() && mission.getResetType() == null) continue;
                // Check cooldown for repeatable
                if (mission.getResetType() == null && !isCooldownExpired(progress, mission)) continue;
                // For daily/weekly: if completed and not yet reset time, skip
                if (mission.getResetType() != null) continue;
                // Reset for new attempt
                progress.reset();
            }

            // Check time limit
            if (mission.getTimeLimitSeconds() > 0 && progress.getStartedAt() > 0) {
                long elapsed = (System.currentTimeMillis() - progress.getStartedAt()) / 1000L;
                if (elapsed > mission.getTimeLimitSeconds()) {
                    // Time expired, reset
                    progress.reset();
                }
            }

            // Check each condition
            boolean anyMatched = false;
            for (MissionConditionImpl condition : mission.getConditionImpls()) {
                if (condition.matches(actionType, actionTarget)) {
                    progress.addConditionProgress(condition.getId(), amount);
                    anyMatched = true;
                }
            }

            if (!anyMatched) continue;

            // Check completion
            if (!progress.isCompleted() && progress.checkCompletion(mission)) {
                progress.markCompleted();

                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.sendMessage("§a§lМиссия выполнена: §e" + mission.getDisplayName());
                    if (island != null) {
                        MissionCompleteEvent event = new MissionCompleteEvent(player, island, mission);
                        Bukkit.getPluginManager().callEvent(event);
                    }
                }
            }
        }
    }

    @Override
    public boolean isCompleted(UUID playerId, String missionId) {
        MissionProgress progress = getProgress(playerId, missionId);
        return progress != null && progress.isCompleted();
    }

    @Override
    public boolean claimReward(Player player, String missionId) {
        Map<String, MissionProgressImpl> map = playerProgress.get(player.getUniqueId());
        if (map == null) return false;

        MissionProgressImpl progress = map.get(missionId);
        if (progress == null || !progress.isCompleted() || progress.isClaimed()) return false;

        MissionImpl mission = missions.get(missionId);
        if (mission == null) return false;

        // Money reward
        if (mission.getMoneyReward() > 0) {
            economy.deposit(player.getUniqueId(), mission.getMoneyReward());
        }

        // Island XP reward
        if (mission.getXpReward() > 0) {
            Island island = islandManager.getPlayerIsland(player.getUniqueId());
            if (island != null) {
                island.addExperience(mission.getXpReward());
            }
        }

        // Item rewards
        for (String itemStr : mission.getItemRewards()) {
            giveItemReward(player, itemStr);
        }

        // Command rewards
        for (String cmd : mission.getCommandRewards()) {
            String resolved = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        progress.markClaimed();
        return true;
    }

    @Override
    public Collection<Mission> getMissionsForLevel(int islandLevel) {
        List<Mission> result = new ArrayList<Mission>();
        for (MissionImpl mission : missions.values()) {
            if (mission.getRequiredLevel() <= islandLevel) {
                result.add(mission);
            }
        }
        return result;
    }

    @Override
    public void resetProgress(UUID playerId, String missionId) {
        Map<String, MissionProgressImpl> map = playerProgress.get(playerId);
        if (map != null) {
            MissionProgressImpl progress = map.get(missionId);
            if (progress != null) {
                progress.reset();
            }
        }
    }

    /**
     * Get all categories from loaded missions.
     */
    public List<String> getCategories() {
        List<String> categories = new ArrayList<String>();
        for (MissionImpl mission : missions.values()) {
            if (!categories.contains(mission.getCategory())) {
                categories.add(mission.getCategory());
            }
        }
        return categories;
    }

    /**
     * Get missions in a specific category.
     */
    public List<MissionImpl> getMissionsByCategory(String category) {
        List<MissionImpl> result = new ArrayList<MissionImpl>();
        for (MissionImpl mission : missions.values()) {
            if (mission.getCategory().equalsIgnoreCase(category)) {
                result.add(mission);
            }
        }
        return result;
    }

    /**
     * Get the progress map for persistence.
     */
    public Map<UUID, Map<String, MissionProgressImpl>> getAllProgress() {
        return playerProgress;
    }

    /**
     * Load progress from persistence.
     */
    public void setAllProgress(Map<UUID, Map<String, MissionProgressImpl>> data) {
        playerProgress.clear();
        playerProgress.putAll(data);
    }

    // --- Private helpers ---

    private MissionProgressImpl getOrCreateProgress(UUID playerId, String missionId) {
        Map<String, MissionProgressImpl> map = playerProgress.get(playerId);
        if (map == null) {
            map = new LinkedHashMap<String, MissionProgressImpl>();
            playerProgress.put(playerId, map);
        }
        MissionProgressImpl progress = map.get(missionId);
        if (progress == null) {
            progress = new MissionProgressImpl(playerId, missionId);
            map.put(missionId, progress);
        }
        return progress;
    }

    private boolean arePrerequisitesMet(UUID playerId, MissionImpl mission) {
        for (String prereq : mission.getPrerequisites()) {
            if (!isCompleted(playerId, prereq)) return false;
        }
        return true;
    }

    private boolean isCooldownExpired(MissionProgressImpl progress, MissionImpl mission) {
        if (mission.getCooldownSeconds() <= 0) return true;
        long lastCompleted = progress.getLastCompletedAt();
        if (lastCompleted == 0L) return true;
        long elapsed = (System.currentTimeMillis() - lastCompleted) / 1000L;
        return elapsed >= mission.getCooldownSeconds();
    }

    /**
     * Check if a daily/weekly mission should be reset based on completion time.
     */
    private boolean shouldResetMission(MissionProgressImpl progress, String resetType) {
        if (!progress.isCompleted()) return false;
        long completedAt = progress.getLastCompletedAt();
        if (completedAt == 0L) return false;

        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar completed = java.util.Calendar.getInstance();
        completed.setTimeInMillis(completedAt);

        if ("daily".equalsIgnoreCase(resetType)) {
            // Reset if completed on a different day
            return now.get(java.util.Calendar.DAY_OF_YEAR) != completed.get(java.util.Calendar.DAY_OF_YEAR)
                    || now.get(java.util.Calendar.YEAR) != completed.get(java.util.Calendar.YEAR);
        } else if ("weekly".equalsIgnoreCase(resetType)) {
            // Reset if completed in a different week
            return now.get(java.util.Calendar.WEEK_OF_YEAR) != completed.get(java.util.Calendar.WEEK_OF_YEAR)
                    || now.get(java.util.Calendar.YEAR) != completed.get(java.util.Calendar.YEAR);
        }
        return false;
    }

    private void giveItemReward(Player player, String itemStr) {
        // Format: "MATERIAL:amount" or "MATERIAL"
        String[] parts = itemStr.split(":");
        Material mat = Material.matchMaterial(parts[0]);
        if (mat == null) return;
        int amount = parts.length > 1 ? parseInt(parts[1], 1) : 1;
        ItemStack stack = new ItemStack(mat, amount);
        player.getInventory().addItem(stack);
    }

    private void loadMissions() {
        File file = new File(plugin.getDataFolder(), "missions.yml");
        if (!file.exists()) {
            plugin.saveResource("missions.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection missionsSection = cfg.getConfigurationSection("missions");
        if (missionsSection == null) return;

        for (String key : missionsSection.getKeys(false)) {
            ConfigurationSection ms = missionsSection.getConfigurationSection(key);
            if (ms == null) continue;

            String displayName = ms.getString("display-name", key);
            String category = ms.getString("category", "general");
            Material icon = Material.matchMaterial(ms.getString("icon", "PAPER"));
            if (icon == null) icon = Material.PAPER;
            int requiredLevel = ms.getInt("required-level", 1);
            List<String> prerequisites = ms.getStringList("prerequisites");
            boolean repeatable = ms.getBoolean("repeatable", false);
            int cooldownSeconds = ms.getInt("cooldown-seconds", 0);
            int timeLimitSeconds = ms.getInt("time-limit-seconds", 0);
            List<String> lore = ms.getStringList("lore");
            String resetType = ms.getString("reset", null);

            // Conditions
            ConditionMode mode = ConditionMode.AND;
            String modeStr = ms.getString("conditions.mode", "AND");
            if ("OR".equalsIgnoreCase(modeStr)) mode = ConditionMode.OR;

            List<MissionConditionImpl> conditions = new ArrayList<MissionConditionImpl>();
            List<?> condList = ms.getList("conditions.list");
            if (condList != null) {
                ConfigurationSection condSection = ms.getConfigurationSection("conditions");
                if (condSection != null) {
                    List<Map<?, ?>> mapList = condSection.getMapList("list");
                    int idx = 0;
                    for (Map<?, ?> condMap : mapList) {
                        String typeStr = String.valueOf(condMap.get("type"));
                        String target = condMap.containsKey("target") ? String.valueOf(condMap.get("target")) : "";
                        int amount = condMap.containsKey("amount") ? parseInt(String.valueOf(condMap.get("amount")), 1) : 1;

                        MissionType type = parseMissionType(typeStr);
                        if (type != null) {
                            String condId = key + "_cond_" + idx;
                            conditions.add(new MissionConditionImpl(condId, type, target, amount));
                            idx++;
                        }
                    }
                }
            }

            // Rewards
            ConfigurationSection rewards = ms.getConfigurationSection("rewards");
            double moneyReward = rewards != null ? rewards.getDouble("money", 0.0) : 0.0;
            long xpReward = rewards != null ? rewards.getLong("island-xp", 0L) : 0L;
            List<String> commandRewards = rewards != null ? rewards.getStringList("commands") : Collections.<String>emptyList();

            List<String> itemRewards = new ArrayList<String>();
            if (rewards != null) {
                List<Map<?, ?>> itemsList = rewards.getMapList("items");
                for (Map<?, ?> itemMap : itemsList) {
                    String material = String.valueOf(itemMap.get("material"));
                    int amount = itemMap.containsKey("amount") ? parseInt(String.valueOf(itemMap.get("amount")), 1) : 1;
                    itemRewards.add(material + ":" + amount);
                }
            }

            MissionImpl mission = new MissionImpl(key, displayName, lore, conditions, mode,
                    moneyReward, xpReward, commandRewards, itemRewards, requiredLevel,
                    repeatable, cooldownSeconds, timeLimitSeconds, prerequisites, icon, category, resetType);
            missions.put(key, mission);
        }

        plugin.getLogger().info("Loaded " + missions.size() + " missions.");
    }

    private MissionType parseMissionType(String str) {
        if (str == null) return null;
        try {
            return MissionType.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
