package me.reil.skybound.core.island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-island player-run shops:
 * Each shop is a chest at a specific location, owned by one island, selling its contents
 * for a fixed price per item stack. Other players click the chest to open the buy GUI.
 */
public final class PlayerShopManager {

    public static final class Shop {
        public final String shopId;
        public final String islandId;
        public final UUID owner;
        public final Location location;
        public double price;

        public Shop(String shopId, String islandId, UUID owner, Location location, double price) {
            this.shopId = shopId;
            this.islandId = islandId;
            this.owner = owner;
            this.location = location;
            this.price = price;
        }
    }

    private final JavaPlugin plugin;
    private final File dataFile;
    /** locKey -> shop */
    private final Map<String, Shop> shops = new LinkedHashMap<String, Shop>();

    public PlayerShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/player-shops.yml");
        load();
    }

    public Shop getShopAt(Location loc) {
        return shops.get(locKey(loc));
    }

    public Shop createShop(Location loc, String islandId, UUID owner, double price) {
        String key = locKey(loc);
        if (shops.containsKey(key)) return null;
        String shopId = UUID.randomUUID().toString().substring(0, 8);
        Shop shop = new Shop(shopId, islandId, owner, loc, price);
        shops.put(key, shop);
        save();
        return shop;
    }

    public boolean removeShop(Location loc) {
        boolean removed = shops.remove(locKey(loc)) != null;
        if (removed) save();
        return removed;
    }

    public List<Shop> getIslandShops(String islandId) {
        List<Shop> out = new ArrayList<Shop>();
        for (Shop s : shops.values()) if (s.islandId.equals(islandId)) out.add(s);
        return out;
    }

    public List<Shop> getAllShops() {
        return new ArrayList<Shop>(shops.values());
    }

    private static String locKey(Location loc) {
        return loc.getWorld().getName() + "@" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public void load() {
        shops.clear();
        if (!dataFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection sec = cfg.getConfigurationSection("shops");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String worldName = sec.getString(id + ".world");
            World w = Bukkit.getWorld(worldName == null ? "world" : worldName);
            if (w == null) continue;
            int x = sec.getInt(id + ".x");
            int y = sec.getInt(id + ".y");
            int z = sec.getInt(id + ".z");
            String islandId = sec.getString(id + ".island");
            UUID owner;
            try { owner = UUID.fromString(sec.getString(id + ".owner")); }
            catch (Exception e) { continue; }
            double price = sec.getDouble(id + ".price", 1.0);
            Location loc = new Location(w, x, y, z);
            Shop shop = new Shop(id, islandId, owner, loc, price);
            shops.put(locKey(loc), shop);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Shop s : shops.values()) {
            String base = "shops." + s.shopId + ".";
            cfg.set(base + "world", s.location.getWorld().getName());
            cfg.set(base + "x", s.location.getBlockX());
            cfg.set(base + "y", s.location.getBlockY());
            cfg.set(base + "z", s.location.getBlockZ());
            cfg.set(base + "island", s.islandId);
            cfg.set(base + "owner", s.owner.toString());
            cfg.set(base + "price", s.price);
        }
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            cfg.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save player-shops.yml: " + ex.getMessage());
        }
    }
}
