package me.reil.skybound.core.island;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player statistics that are useful for /is stats:
 * blocks broken, blocks placed, mobs killed, fish caught, money spent/earned,
 * playtime, distance walked, missions completed, etc.
 */
public final class PlayerStatsManager {

    public enum Stat {
        BLOCKS_BROKEN,
        BLOCKS_PLACED,
        MOBS_KILLED,
        DEATHS,
        FISH_CAUGHT,
        ITEMS_CRAFTED,
        ITEMS_SMELTED,
        MISSIONS_COMPLETED,
        SHOP_BUYS,
        SHOP_SELLS,
        BANK_DEPOSITS,
        BANK_WITHDRAWS,
        XP_GAINED,
        DISTANCE_WALKED,
        PLAYTIME_SECONDS,
        ISLAND_VISITS,
        EVENTS_COMPLETED;
    }

    private final JavaPlugin plugin;
    private final File dataFile;
    /** playerId -> (stat -> value) */
    private final Map<UUID, Map<Stat, Long>> stats = new LinkedHashMap<UUID, Map<Stat, Long>>();
    /** playerId -> last login timestamp (for playtime tracking) */
    private final Map<UUID, Long> sessionStart = new LinkedHashMap<UUID, Long>();

    public PlayerStatsManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/player-stats.yml");
        load();
    }

    public long get(UUID playerId, Stat stat) {
        Map<Stat, Long> m = stats.get(playerId);
        if (m == null) return 0L;
        Long v = m.get(stat);
        return v == null ? 0L : v;
    }

    public void increment(UUID playerId, Stat stat, long amount) {
        Map<Stat, Long> m = stats.get(playerId);
        if (m == null) {
            m = new LinkedHashMap<Stat, Long>();
            stats.put(playerId, m);
        }
        Long cur = m.get(stat);
        m.put(stat, (cur == null ? 0L : cur) + amount);
    }

    public void set(UUID playerId, Stat stat, long value) {
        Map<Stat, Long> m = stats.get(playerId);
        if (m == null) {
            m = new LinkedHashMap<Stat, Long>();
            stats.put(playerId, m);
        }
        m.put(stat, value);
    }

    public Map<Stat, Long> getAll(UUID playerId) {
        Map<Stat, Long> m = stats.get(playerId);
        return m == null ? new LinkedHashMap<Stat, Long>() : new LinkedHashMap<Stat, Long>(m);
    }

    /** Mark session start; combine with onPlayerQuit to compute playtime delta. */
    public void onPlayerJoin(Player player) {
        sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void onPlayerQuit(Player player) {
        Long start = sessionStart.remove(player.getUniqueId());
        if (start != null) {
            long sec = (System.currentTimeMillis() - start) / 1000L;
            if (sec > 0) increment(player.getUniqueId(), Stat.PLAYTIME_SECONDS, sec);
        }
    }

    /** Flush sessions to stats (called periodically + on shutdown). */
    public void flushSessions() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> e : sessionStart.entrySet()) {
            long sec = (now - e.getValue()) / 1000L;
            if (sec > 0) {
                increment(e.getKey(), Stat.PLAYTIME_SECONDS, sec);
                e.setValue(now);
            }
        }
    }

    // --- Persistence ---

    public void load() {
        stats.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("stats");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                Map<Stat, Long> m = new LinkedHashMap<Stat, Long>();
                ConfigurationSection inner = sec.getConfigurationSection(key);
                if (inner == null) continue;
                for (String statKey : inner.getKeys(false)) {
                    try {
                        Stat s = Stat.valueOf(statKey);
                        m.put(s, inner.getLong(statKey));
                    } catch (IllegalArgumentException ignored) {}
                }
                stats.put(id, m);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        flushSessions();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Map<Stat, Long>> e : stats.entrySet()) {
            for (Map.Entry<Stat, Long> s : e.getValue().entrySet()) {
                cfg.set("stats." + e.getKey() + "." + s.getKey().name(), s.getValue());
            }
        }
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save player-stats.yml: " + ex.getMessage());
        }
    }
}
