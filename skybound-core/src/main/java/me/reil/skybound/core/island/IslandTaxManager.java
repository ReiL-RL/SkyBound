package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Periodic island tax collected from the island bank.
 * Globally toggleable. If bank balance is below the tax amount, the island
 * decays — loses level XP / value points.
 */
public final class IslandTaxManager {

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final File dataFile;
    private BukkitTask task;

    public IslandTaxManager(JavaPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.dataFile = new File(plugin.getDataFolder(), "data/island-tax.yml");
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("tax.enabled", false);
    }

    public void setEnabled(boolean enabled) {
        plugin.getConfig().set("tax.enabled", enabled);
        plugin.saveConfig();
    }

    /** Tax interval in seconds. */
    public long getIntervalSeconds() {
        return Math.max(60L, plugin.getConfig().getLong("tax.interval-seconds", 86400L)); // daily by default
    }

    /** Flat tax per island per interval. */
    public double getFlatTax() {
        return Math.max(0.0, plugin.getConfig().getDouble("tax.flat-amount", 100.0));
    }

    /** Percentage of bank balance to take additionally. */
    public double getPercentTax() {
        return Math.max(0.0, plugin.getConfig().getDouble("tax.percent", 0.5)); // 0.5%
    }

    /** XP penalty when bank can't pay. */
    public long getXpPenalty() {
        return Math.max(0L, plugin.getConfig().getLong("tax.xp-penalty", 100L));
    }

    public void start() {
        stop();
        if (!isEnabled()) {
            plugin.getLogger().info("Island tax: disabled.");
            return;
        }
        // Check every minute, but actually charge based on per-island timers
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(), 1200L, 1200L);
        plugin.getLogger().info("Island tax: enabled, interval=" + getIntervalSeconds() + "s");
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Last-charge timestamps per island. */
    private final Map<String, Long> lastChargedAt = new LinkedHashMap<String, Long>();

    private void tick() {
        if (!isEnabled()) {
            stop();
            return;
        }
        long now = System.currentTimeMillis();
        long intervalMs = getIntervalSeconds() * 1000L;
        for (Island island : islandManager.getAllIslands()) {
            Long last = lastChargedAt.get(island.getId());
            if (last == null) {
                lastChargedAt.put(island.getId(), now);
                continue;
            }
            if (now - last >= intervalMs) {
                charge(island);
                lastChargedAt.put(island.getId(), now);
            }
        }
        islandManager.saveData();
        save();
    }

    private void charge(Island island) {
        double tax = getFlatTax() + (island.getBankBalance() * getPercentTax() / 100.0);
        if (Double.isNaN(tax) || Double.isInfinite(tax) || tax <= 0.0) return;
        double bank = island.getBankBalance();
        if (bank >= tax) {
            island.setBankBalance(bank - tax);
            notifyMembers(island, "tax.charged", "{amount}", String.format("%.2f", tax));
        } else {
            island.setBankBalance(0.0);
            // XP penalty
            long penalty = getXpPenalty();
            if (penalty > 0) {
                long currentXp = island.getExperience();
                // We can't directly subtract via API; addExperience adds. The penalty
                // is visualised as level decay if implementation supports it.
                // Best-effort: set level back if possible.
                try {
                    java.lang.reflect.Method setExp = island.getClass().getMethod("setExperience", long.class);
                    setExp.invoke(island, Math.max(0L, currentXp - penalty));
                } catch (Exception ignored) {}
            }
            notifyMembers(island, "tax.penalty");
        }
    }

    private void notifyMembers(Island island, String key, String... replacements) {
        for (UUID id : island.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && plugin instanceof SkyBoundPlugin) {
                ((SkyBoundPlugin) plugin).getLangManager().send(p, key, replacements);
            }
        }
    }

    // --- Persistence ---

    public void load() {
        lastChargedAt.clear();
        if (!dataFile.exists()) return;
        org.bukkit.configuration.file.YamlConfiguration cfg =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(dataFile);
        org.bukkit.configuration.ConfigurationSection sec = cfg.getConfigurationSection("last-charged");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            lastChargedAt.put(key, sec.getLong(key));
        }
    }

    public void save() {
        org.bukkit.configuration.file.YamlConfiguration cfg =
                new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<String, Long> e : lastChargedAt.entrySet()) {
            cfg.set("last-charged." + e.getKey(), e.getValue());
        }
        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "island-tax.yml");
    }
}
