package me.reil.skybound.core.storage;

import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.island.IslandImpl;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Saves and loads island data to/from YAML.
 */
public final class IslandDataStore {

    private final JavaPlugin plugin;
    private final File file;

    public IslandDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/islands.yml");
    }

    public Map<String, IslandImpl> load() {
        Map<String, IslandImpl> islands = new LinkedHashMap<String, IslandImpl>();
        if (!file.exists()) return islands;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("islands");
        if (section == null) return islands;

        for (String id : section.getKeys(false)) {
            ConfigurationSection is = section.getConfigurationSection(id);
            if (is == null) continue;

            UUID owner = UUID.fromString(is.getString("owner"));
            Location center = deserializeLocation(is.getConfigurationSection("center"));
            int radius = is.getInt("radius", 60);

            IslandImpl island = new IslandImpl(id, owner, center, radius);
            island.setName(is.getString("name", id));
            island.setDescription(is.getString("description", ""));
            island.setLevel(is.getInt("level", 1));
            island.addExperience(is.getLong("experience", 0L) - island.getExperience());
            island.setBankBalance(is.getDouble("bank-balance", 0.0));
            island.setLocked(is.getBoolean("locked", false));

            Location home = deserializeLocation(is.getConfigurationSection("home"));
            if (home != null) island.setHome(home);

            // Schematic name
            island.setSchematicName(is.getString("schematic-name", null));

            // Members
            ConfigurationSection members = is.getConfigurationSection("members");
            if (members != null) {
                for (String uuidStr : members.getKeys(false)) {
                    UUID memberId = UUID.fromString(uuidStr);
                    IslandRole role = IslandRole.valueOf(members.getString(uuidStr, "MEMBER"));
                    if (!memberId.equals(owner)) {
                        island.addMember(memberId, role);
                    }
                }
            }

            // Warps
            ConfigurationSection warps = is.getConfigurationSection("warps");
            if (warps != null) {
                for (String warpName : warps.getKeys(false)) {
                    Location warpLoc = deserializeLocation(warps.getConfigurationSection(warpName));
                    if (warpLoc != null) island.setWarp(warpName, warpLoc);
                }
            }

            // Settings
            ConfigurationSection settings = is.getConfigurationSection("settings");
            if (settings != null) {
                for (String key : settings.getKeys(false)) {
                    island.setSetting(key, settings.get(key));
                }
            }

            islands.put(id, island);
        }

        plugin.getLogger().info("Loaded " + islands.size() + " islands.");
        return islands;
    }

    public void save(Map<String, IslandImpl> islands) {
        YamlConfiguration cfg = new YamlConfiguration();

        for (Map.Entry<String, IslandImpl> entry : islands.entrySet()) {
            IslandImpl island = entry.getValue();
            String path = "islands." + island.getId();

            cfg.set(path + ".owner", island.getOwner().toString());
            cfg.set(path + ".name", island.getName());
            cfg.set(path + ".description", island.getDescription());
            cfg.set(path + ".level", island.getLevel());
            cfg.set(path + ".experience", island.getExperience());
            cfg.set(path + ".radius", island.getRadius());
            cfg.set(path + ".bank-balance", island.getBankBalance());
            cfg.set(path + ".locked", island.isLocked());
            cfg.set(path + ".schematic-name", island.getSchematicName());

            serializeLocation(cfg, path + ".center", island.getCenter());
            serializeLocation(cfg, path + ".home", island.getHome());

            // Members
            Set<UUID> members = island.getMembers();
            for (UUID memberId : members) {
                cfg.set(path + ".members." + memberId.toString(), island.getMemberRole(memberId).name());
            }

            // Warps
            Map<String, Location> warps = island.getWarps();
            for (Map.Entry<String, Location> warp : warps.entrySet()) {
                serializeLocation(cfg, path + ".warps." + warp.getKey(), warp.getValue());
            }

            // Settings
            Map<String, Object> settings = island.getSettings();
            for (Map.Entry<String, Object> setting : settings.entrySet()) {
                cfg.set(path + ".settings." + setting.getKey(), setting.getValue());
            }
        }

        saveFile(cfg);
    }

    private void serializeLocation(YamlConfiguration cfg, String path, Location loc) {
        if (loc == null) return;
        cfg.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        cfg.set(path + ".x", loc.getX());
        cfg.set(path + ".y", loc.getY());
        cfg.set(path + ".z", loc.getZ());
        cfg.set(path + ".yaw", (double) loc.getYaw());
        cfg.set(path + ".pitch", (double) loc.getPitch());
    }

    private Location deserializeLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldName = section.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void saveFile(YamlConfiguration cfg) {
        try {
            file.getParentFile().mkdirs();
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save islands.yml: " + e.getMessage());
        }
    }
}
