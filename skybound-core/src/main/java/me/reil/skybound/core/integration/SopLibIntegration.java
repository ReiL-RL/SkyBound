package me.reil.skybound.core.integration;

import net.enelson.sopli.lib.SopLib;
import net.enelson.sopli.lib.database.DatabaseConfig;
import net.enelson.sopli.lib.database.DatabaseService;
import net.enelson.sopli.lib.database.SopDatabase;
import net.enelson.sopli.lib.item.ItemUtils;
import net.enelson.sopli.lib.text.TextUtils;
import net.enelson.sopli.lib.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Integration with SopLib for multi-version support.
 * Provides:
 * - Version-safe item creation (NBT, custom model data, heads)
 * - HikariCP database pooling
 * - Hex color / MiniMessage text formatting
 * - Location serialization across versions
 * - Protection checks
 *
 * When SopLib is not present, falls back to basic Bukkit API.
 */
public final class SopLibIntegration {

    private final JavaPlugin plugin;
    private boolean available;
    private SopLib sopLib;

    public SopLibIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.available = false;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("SopLib")) {
                this.sopLib = SopLib.getInstance();
                if (this.sopLib != null) {
                    this.available = true;
                    plugin.getLogger().info("SopLib integration enabled (multi-version support active).");
                }
            }
        } catch (NoClassDefFoundError e) {
            // SopLib not on classpath
        }

        if (!available) {
            plugin.getLogger().info("SopLib not found. Using basic Bukkit API (single-version mode).");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    // === Database ===

    /**
     * Create a MySQL database connection pool via SopLib's HikariCP wrapper.
     * @return SopDatabase instance, or null if SopLib unavailable
     */
    public SopDatabase createDatabase(String host, int port, String database, String username, String password, String poolName) {
        if (!available) return null;
        DatabaseConfig config = DatabaseConfig.mysql(host, port, database)
                .credentials(username, password)
                .poolName(poolName)
                .maximumPoolSize(10)
                .minimumIdle(2)
                .build();
        return sopLib.getDatabaseService().createDatabase(config);
    }

    // === Items (multi-version safe) ===

    /**
     * Create an ItemStack with version-safe NBT, model data, and enchantments.
     */
    public ItemStack createItem(String material, int amount, Object modelData, String name, List<String> enchantments, List<String> lore, List<String> nbts) {
        if (!available) return null;
        return sopLib.getItemUtils().createItem(material, amount, modelData, name, enchantments, lore, nbts);
    }

    /**
     * Get a player head by texture value (version-safe).
     */
    public ItemStack getHead(String textureValue, String displayName) {
        if (!available) return null;
        return sopLib.getItemUtils().getHeadTexture(textureValue, displayName);
    }

    /**
     * Set custom model data on an item (version-safe, handles 1.14+ and 1.21.2+ component model).
     */
    public void setCustomModelData(ItemStack item, Object... model) {
        if (!available || item == null) return;
        sopLib.getItemUtils().setCustomModelData(item, model);
    }

    /**
     * Set a custom NBT key on an item for identification.
     */
    public void setCustomItemKey(ItemStack item, String key, String fallback) {
        if (!available || item == null) return;
        sopLib.getItemUtils().setCustomItemKey(item, key, fallback);
    }

    /**
     * Get the custom NBT key from an item.
     */
    public String getCustomItemKey(ItemStack item) {
        if (!available || item == null) return null;
        return sopLib.getItemUtils().getCustomItemKey(item);
    }

    // === Text (hex colors, MiniMessage) ===

    /**
     * Colorize text with hex colors (&#RRGGBB), MiniMessage tags, and legacy & codes.
     * Works on all server versions.
     */
    public String colorize(String text) {
        if (!available || text == null) {
            // Fallback: basic & color codes only
            if (text == null) return "";
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
        }
        return sopLib.getTextUtils().color(text);
    }

    /**
     * Colorize a list of strings.
     */
    public List<String> colorize(List<String> lines) {
        if (!available || lines == null) {
            if (lines == null) return java.util.Collections.emptyList();
            java.util.List<String> result = new java.util.ArrayList<String>();
            for (String line : lines) {
                result.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            return result;
        }
        return sopLib.getTextUtils().color(lines);
    }

    // === Util (location serialization, versioned messages) ===

    /**
     * Serialize a location to a compact string (version-safe).
     */
    public String serializeLocation(org.bukkit.Location location) {
        if (!available || location == null) {
            // Fallback
            if (location == null) return "";
            return location.getWorld().getName() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ()
                    + ":" + location.getYaw() + ":" + location.getPitch();
        }
        return sopLib.getUtil().getSerializedLocation(location);
    }

    /**
     * Deserialize a location from a compact string (version-safe).
     */
    public org.bukkit.Location deserializeLocation(String serialized) {
        if (!available || serialized == null || serialized.isEmpty()) {
            return null;
        }
        return sopLib.getUtil().getDeserializedLocation(serialized);
    }

    /**
     * Send a version-safe message to a player (handles Adventure/legacy).
     */
    public void sendMessage(org.bukkit.entity.Player player, String message) {
        if (!available || player == null || message == null) {
            if (player != null && message != null) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            }
            return;
        }
        sopLib.getUtil().sendVersionedMessage(player, message);
    }

    // === Protection ===

    /**
     * Check if a player can build at a location (integrates with WorldGuard etc via SopLib).
     */
    public boolean canBuild(org.bukkit.entity.Player player, org.bukkit.Location location) {
        if (!available) return true; // No protection check without SopLib
        return sopLib.getProtectionService().canBuild(player, location);
    }

    /**
     * Shutdown SopLib database connections.
     */
    public void shutdown() {
        // SopLib manages its own lifecycle via its plugin
    }
}
