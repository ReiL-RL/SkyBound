package me.reil.skybound.core.visit;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.visit.VisitProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages island visits and likes/rating system.
 * Players can like an island once per day.
 */
public final class VisitManager implements VisitProvider {

    private final JavaPlugin plugin;
    private final Map<String, Integer> likes = new LinkedHashMap<String, Integer>();
    private final Map<String, Set<String>> dailyLikes = new LinkedHashMap<String, Set<String>>();
    private int lastResetDay = -1;
    private BukkitTask resetTask;

    public VisitManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadData();
        startDailyResetCheck();
    }

    @Override
    public void visit(Player player, Island island) {
        if (island.isLocked()) {
            player.sendMessage("§cЭтот остров закрыт для посещений.");
            return;
        }
        player.teleport(island.getHome());
        player.sendMessage("§aВы посетили остров §e" + island.getName() + "§a!");
    }

    @Override
    public boolean like(Player player, Island island) {
        if (hasLikedToday(player, island)) {
            return false;
        }

        // Can't like own island
        if (island.getMembers().contains(player.getUniqueId())) {
            return false;
        }

        String islandId = island.getId();
        String playerId = player.getUniqueId().toString();

        // Add daily like record
        Set<String> likedBy = dailyLikes.get(islandId);
        if (likedBy == null) {
            likedBy = new HashSet<String>();
            dailyLikes.put(islandId, likedBy);
        }
        likedBy.add(playerId);

        // Increment total likes
        Integer current = likes.get(islandId);
        likes.put(islandId, (current == null ? 0 : current) + 1);

        saveData();
        return true;
    }

    @Override
    public int getLikes(Island island) {
        Integer count = likes.get(island.getId());
        return count == null ? 0 : count;
    }

    @Override
    public boolean hasLikedToday(Player player, Island island) {
        Set<String> likedBy = dailyLikes.get(island.getId());
        if (likedBy == null) return false;
        return likedBy.contains(player.getUniqueId().toString());
    }

    /**
     * Get likes map for leaderboard integration.
     */
    public Map<String, Integer> getAllLikes() {
        return likes;
    }

    public void shutdown() {
        if (resetTask != null) resetTask.cancel();
        saveData();
    }

    private void startDailyResetCheck() {
        // Check every 5 minutes (6000 ticks)
        this.resetTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                if (lastResetDay != today) {
                    dailyLikes.clear();
                    lastResetDay = today;
                    plugin.getLogger().info("Daily likes reset.");
                }
            }
        }, 6000L, 6000L);
    }

    private void loadData() {
        File file = new File(plugin.getDataFolder(), "visits.yml");
        if (!file.exists()) return;

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        this.lastResetDay = cfg.getInt("last-reset-day", -1);

        ConfigurationSection likesSection = cfg.getConfigurationSection("likes");
        if (likesSection != null) {
            for (String key : likesSection.getKeys(false)) {
                likes.put(key, likesSection.getInt(key, 0));
            }
        }

        ConfigurationSection dailySection = cfg.getConfigurationSection("daily-likes");
        if (dailySection != null) {
            for (String islandId : dailySection.getKeys(false)) {
                Set<String> players = new HashSet<String>(dailySection.getStringList(islandId));
                dailyLikes.put(islandId, players);
            }
        }

        plugin.getLogger().info("Loaded visit data for " + likes.size() + " islands.");
    }

    private void saveData() {
        File file = new File(plugin.getDataFolder(), "visits.yml");
        FileConfiguration cfg = new YamlConfiguration();

        cfg.set("last-reset-day", lastResetDay);

        for (Map.Entry<String, Integer> entry : likes.entrySet()) {
            cfg.set("likes." + entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Set<String>> entry : dailyLikes.entrySet()) {
            cfg.set("daily-likes." + entry.getKey(), new java.util.ArrayList<String>(entry.getValue()));
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save visits data: " + e.getMessage());
        }
    }
}
