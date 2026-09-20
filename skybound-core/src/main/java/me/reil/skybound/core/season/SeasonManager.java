package me.reil.skybound.core.season;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.leaderboard.LeaderboardEntry;
import me.reil.skybound.api.season.Season;
import me.reil.skybound.api.season.SeasonProvider;
import me.reil.skybound.api.season.SeasonReward;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandImpl;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.leaderboard.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Manages seasons: time periods with leaderboard rewards.
 * Checks every minute if the season has ended.
 */
public final class SeasonManager implements SeasonProvider {

    private final JavaPlugin plugin;
    private final SeasonConfig config;
    private final IslandManager islandManager;
    private final LeaderboardManager leaderboardManager;
    private SeasonImpl currentSeason;
    private BukkitTask tickTask;

    public SeasonManager(JavaPlugin plugin, SeasonConfig config, IslandManager islandManager, LeaderboardManager leaderboardManager) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.leaderboardManager = leaderboardManager;

        if (config.isEnabled()) {
            loadCurrentSeason();
            startTick();
        }
    }

    @Override
    public Season getCurrentSeason() {
        return currentSeason;
    }

    @Override
    public long getRemainingTime() {
        if (currentSeason == null) return 0L;
        long remaining = currentSeason.getEndTime() - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SeasonReward> getRewards() {
        List<SeasonReward> result = new ArrayList<SeasonReward>();
        for (Map.Entry<Integer, List<String>> entry : config.getRewards().entrySet()) {
            result.add(new SeasonRewardImpl(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public void endSeason() {
        if (currentSeason == null) return;

        plugin.getLogger().info("Season " + currentSeason.getNumber() + " ending...");

        // Recalculate leaderboard
        leaderboardManager.recalculate();

        // Give rewards to top islands
        List<LeaderboardEntry> top = leaderboardManager.getTopByValue(getMaxRewardRank());
        for (Map.Entry<Integer, List<String>> entry : config.getRewards().entrySet()) {
            int rank = entry.getKey();
            if (rank > top.size()) continue;

            LeaderboardEntry leaderEntry = top.get(rank - 1);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(leaderEntry.getOwner());
            String playerName = owner.getName() != null ? owner.getName() : "Unknown";

            for (String cmd : entry.getValue()) {
                String resolved = cmd
                        .replace("{player}", playerName)
                        .replace("{rank}", String.valueOf(rank))
                        .replace("{island}", leaderEntry.getIslandName())
                        .replace("{value}", String.format("%.0f", leaderEntry.getValue()))
                        .replace("{season}", String.valueOf(currentSeason.getNumber()));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
        }

        // Announce
        if (config.isAnnounceEnd()) {
            broadcastLang("season.end.separator");
            broadcastLang("season.end.title", "{season}", String.valueOf(currentSeason.getNumber()));
            for (int i = 0; i < Math.min(3, top.size()); i++) {
                LeaderboardEntry e = top.get(i);
                broadcastLang("season.end.top-entry",
                        "{rank}", String.valueOf(i + 1),
                        "{island}", e.getIslandName(),
                        "{value}", String.format("%.0f", e.getValue()));
            }
            broadcastLang("season.end.separator");
        }

        // Auto-reset islands if configured
        if (config.isAutoResetIslands()) {
            resetIslandsForNewSeason(new ArrayList<Island>(islandManager.getAllIslands()));
        }

        // Start new season
        int nextNumber = currentSeason.getNumber() + 1;
        currentSeason = new SeasonImpl(nextNumber, System.currentTimeMillis(), config.getDurationDays());
        saveCurrentSeason();

        plugin.getLogger().info("Season " + nextNumber + " started.");
    }

    private void resetIslandsForNewSeason(final List<Island> islands) {
        if (!(plugin instanceof SkyBoundPlugin)) {
            plugin.getLogger().warning("Cannot paste season reset schematics: plugin instance is not SkyBoundPlugin.");
            return;
        }
        resetNextIsland(islands, 0);
    }

    private void resetNextIsland(final List<Island> islands, final int index) {
        if (index >= islands.size()) {
            plugin.getLogger().info("All islands reset for new season.");
            return;
        }

        final Island island = islands.get(index);
        final String schematicName = getSeasonResetSchematic(island);
        boolean started = islandManager.regenerateIslandBatched(island.getId(), schematicName, new Runnable() {
            @Override
            public void run() {
                ((SkyBoundPlugin) plugin).getSchematicService().paste(schematicName, island.getCenter());
                resetNextIsland(islands, index + 1);
            }
        });

        if (!started) {
            resetNextIsland(islands, index + 1);
        }
    }

    private String getSeasonResetSchematic(Island island) {
        if (island instanceof IslandImpl) {
            String saved = ((IslandImpl) island).getSchematicName();
            if (saved != null && !saved.isEmpty()) {
                return saved;
            }
        }
        return "desert.schem";
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    private int getMaxRewardRank() {
        int max = 0;
        for (Integer rank : config.getRewards().keySet()) {
            if (rank != null && rank > max) {
                max = rank;
            }
        }
        return max;
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        saveCurrentSeason();
    }

    private void startTick() {
        // Check every minute (1200 ticks)
        this.tickTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (currentSeason != null && !currentSeason.isActive()) {
                    endSeason();
                }
            }
        }, 1200L, 1200L);
    }

    private void loadCurrentSeason() {
        File file = new File(plugin.getDataFolder(), "season-data.yml");
        if (!file.exists()) {
            // Start first season
            currentSeason = new SeasonImpl(1, System.currentTimeMillis(), config.getDurationDays());
            saveCurrentSeason();
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int number = cfg.getInt("current-season.number", 1);
        long startTime = cfg.getLong("current-season.start-time", System.currentTimeMillis());
        currentSeason = new SeasonImpl(number, startTime, config.getDurationDays());
    }

    private void saveCurrentSeason() {
        if (currentSeason == null) return;
        File file = new File(plugin.getDataFolder(), "season-data.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("current-season.number", currentSeason.getNumber());
        cfg.set("current-season.start-time", currentSeason.getStartTime());
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, file, "season-data.yml");
    }

    private void broadcastLang(String key, String... replacements) {
        if (plugin instanceof SkyBoundPlugin) {
            Bukkit.broadcastMessage(((SkyBoundPlugin) plugin).getLangManager().get(key, replacements));
        }
    }
}
