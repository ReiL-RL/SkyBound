package me.reil.skybound.core.season;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.leaderboard.LeaderboardEntry;
import me.reil.skybound.api.season.Season;
import me.reil.skybound.api.season.SeasonProvider;
import me.reil.skybound.api.season.SeasonReward;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.leaderboard.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
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
        List<LeaderboardEntry> top = leaderboardManager.getTopByValue(config.getRewards().size());
        for (Map.Entry<Integer, List<String>> entry : config.getRewards().entrySet()) {
            int rank = entry.getKey();
            if (rank > top.size()) continue;

            LeaderboardEntry leaderEntry = top.get(rank - 1);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(leaderEntry.getOwner());
            String playerName = owner.getName() != null ? owner.getName() : "Unknown";

            for (String cmd : entry.getValue()) {
                String resolved = cmd.replace("{player}", playerName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
        }

        // Announce
        if (config.isAnnounceEnd()) {
            Bukkit.broadcastMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            Bukkit.broadcastMessage("§e§lСезон " + currentSeason.getNumber() + " завершён!");
            for (int i = 0; i < Math.min(3, top.size()); i++) {
                LeaderboardEntry e = top.get(i);
                Bukkit.broadcastMessage("§7 " + (i + 1) + ". §f" + e.getIslandName() + " §7- §e" + String.format("%.0f", e.getValue()));
            }
            Bukkit.broadcastMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
        }

        // Auto-reset islands if configured
        if (config.isAutoResetIslands()) {
            for (Island island : islandManager.getAllIslands()) {
                islandManager.regenerateIsland(island.getId(), "default");
            }
            plugin.getLogger().info("All islands reset for new season.");
        }

        // Start new season
        int nextNumber = currentSeason.getNumber() + 1;
        currentSeason = new SeasonImpl(nextNumber, System.currentTimeMillis(), config.getDurationDays());
        saveCurrentSeason();

        plugin.getLogger().info("Season " + nextNumber + " started.");
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
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

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        int number = cfg.getInt("current-season.number", 1);
        long startTime = cfg.getLong("current-season.start-time", System.currentTimeMillis());
        currentSeason = new SeasonImpl(number, startTime, config.getDurationDays());
    }

    private void saveCurrentSeason() {
        if (currentSeason == null) return;
        File file = new File(plugin.getDataFolder(), "season-data.yml");
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("current-season.number", currentSeason.getNumber());
        cfg.set("current-season.start-time", currentSeason.getStartTime());
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save season data: " + e.getMessage());
        }
    }
}
