package me.reil.skybound.core.booster;

import me.reil.skybound.api.booster.ActiveBooster;
import me.reil.skybound.api.booster.Booster;
import me.reil.skybound.api.booster.BoosterProvider;
import me.reil.skybound.api.booster.BoosterType;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.bank.BankManager;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BoosterManager implements BoosterProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    private final BankManager bankManager;
    private final Map<String, BoosterImpl> boosters = new LinkedHashMap<String, BoosterImpl>();
    private final Map<String, List<ActiveBoosterImpl>> activeBoosters = new LinkedHashMap<String, List<ActiveBoosterImpl>>();

    public BoosterManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager, BankManager bankManager) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.bankManager = bankManager;
        loadBoosters();
    }

    public void reload() {
        boosters.clear();
        loadBoosters();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Booster> getBoosters() {
        return Collections.unmodifiableCollection((Collection<? extends Booster>) (Collection<?>) boosters.values());
    }

    @Override
    public Booster getBooster(String boosterId) {
        return boosters.get(boosterId);
    }

    @Override
    public boolean purchase(Player buyer, Island island, String boosterId) {
        BoosterImpl booster = boosters.get(boosterId);
        if (booster == null || island == null) return false;

        if (!bankManager.withdrawInternal(island, booster.getCost())) return false;

        long now = System.currentTimeMillis();
        ActiveBoosterImpl active = new ActiveBoosterImpl(boosterId, now, now + (booster.getDurationSeconds() * 1000L), booster.getMultiplier());

        List<ActiveBoosterImpl> list = activeBoosters.get(island.getId());
        if (list == null) {
            list = new ArrayList<ActiveBoosterImpl>();
            activeBoosters.put(island.getId(), list);
        }
        list.add(active);
        return true;
    }

    @Override
    public boolean isActive(Island island, String boosterId) {
        return getMultiplier(island, boosterId) > 1.0 || (boosterId.equals("flight") && getRemainingSeconds(island, boosterId) > 0);
    }

    @Override
    public long getRemainingSeconds(Island island, String boosterId) {
        if (island == null) return 0L;
        List<ActiveBoosterImpl> list = activeBoosters.get(island.getId());
        if (list == null) return 0L;

        long now = System.currentTimeMillis();
        for (ActiveBoosterImpl active : list) {
            if (active.getBoosterId().equals(boosterId) && active.getExpiresAt() > now) {
                return (active.getExpiresAt() - now) / 1000L;
            }
        }
        return 0L;
    }

    @Override
    public double getMultiplier(Island island, String boosterId) {
        if (island == null) return 1.0;
        List<ActiveBoosterImpl> list = activeBoosters.get(island.getId());
        if (list == null) return 1.0;

        long now = System.currentTimeMillis();
        for (ActiveBoosterImpl active : list) {
            if (active.getBoosterId().equals(boosterId) && active.getExpiresAt() > now) {
                return active.getMultiplier();
            }
        }
        return 1.0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ActiveBooster> getActiveBoosters(Island island) {
        if (island == null) return Collections.emptyList();
        List<ActiveBoosterImpl> list = activeBoosters.get(island.getId());
        if (list == null) return Collections.emptyList();

        long now = System.currentTimeMillis();
        List<ActiveBooster> result = new ArrayList<ActiveBooster>();
        for (ActiveBoosterImpl active : list) {
            if (active.getExpiresAt() > now) {
                result.add(active);
            }
        }
        return result;
    }

    public Map<String, List<ActiveBoosterImpl>> getAllActiveBoosters() {
        return activeBoosters;
    }

    public void setAllActiveBoosters(Map<String, List<ActiveBoosterImpl>> data) {
        activeBoosters.clear();
        activeBoosters.putAll(data);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (List<ActiveBoosterImpl> list : activeBoosters.values()) {
            Iterator<ActiveBoosterImpl> it = list.iterator();
            while (it.hasNext()) {
                if (it.next().getExpiresAt() <= now) {
                    it.remove();
                }
            }
        }
    }

    private void loadBoosters() {
        File file = new File(plugin.getDataFolder(), "boosters.yml");
        if (!file.exists()) {
            plugin.saveResource("boosters.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("boosters");
        if (section == null) return;

        for (String boosterId : section.getKeys(false)) {
            ConfigurationSection bs = section.getConfigurationSection(boosterId);
            if (bs == null) continue;

            String displayName = bs.getString("display-name", boosterId);
            String description = bs.getString("description", "");
            Material icon = Material.matchMaterial(bs.getString("icon", "PAPER"));
            if (icon == null) icon = Material.PAPER;
            BoosterType type = parseBoosterType(bs.getString("type", "CUSTOM"));
            int duration = bs.getInt("duration-seconds", 600);
            double cost = bs.getDouble("cost", 5000.0);
            double multiplier = bs.getDouble("multiplier", 2.0);

            boosters.put(boosterId, new BoosterImpl(boosterId, displayName, description, duration, cost, multiplier, type, icon));
        }

        plugin.getLogger().info("Loaded " + boosters.size() + " boosters.");
    }

    private BoosterType parseBoosterType(String str) {
        try {
            return BoosterType.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BoosterType.CUSTOM;
        }
    }
}
