package me.reil.skybound.core.integration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SopLib runtime integration for multi-version support.
 *
 * Reflection keeps SopLib out of the Maven compile classpath, so the project can be
 * built without publishing SopLib to Maven. On the server, SopLib is a required
 * runtime dependency and acts as SkyBound's multi-version compatibility core.
 */
public final class SopLibIntegration {

    private final JavaPlugin plugin;
    private boolean available;
    private Object sopLib;

    public SopLibIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.available = false;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("SopLib")) {
                Class<?> sopLibClass = Class.forName("net.enelson.sopli.lib.SopLib");
                this.sopLib = sopLibClass.getMethod("getInstance").invoke(null);
                if (this.sopLib != null) {
                    this.available = true;
                    plugin.getLogger().info("SopLib integration enabled (multi-version support active).");
                }
            }
        } catch (Throwable ignored) {
            this.available = false;
            this.sopLib = null;
        }

        if (!available) {
            plugin.getLogger().warning("SopLib runtime bridge is not available. Check that the required SopLib plugin is installed and enabled.");
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public Object createDatabase(String host, int port, String database, String username, String password, String poolName) {
        if (!available) return null;
        try {
            Class<?> configClass = Class.forName("net.enelson.sopli.lib.database.DatabaseConfig");
            Object builder = configClass.getMethod("mysql", String.class, int.class, String.class)
                    .invoke(null, host, port, database);
            builder = invoke(builder, "credentials", new Class<?>[]{String.class, String.class}, username, password);
            builder = invoke(builder, "poolName", new Class<?>[]{String.class}, poolName);
            builder = invoke(builder, "maximumPoolSize", new Class<?>[]{int.class}, 10);
            builder = invoke(builder, "minimumIdle", new Class<?>[]{int.class}, 2);
            Object config = invoke(builder, "build", new Class<?>[0]);
            Object databaseService = invoke(sopLib, "getDatabaseService", new Class<?>[0]);
            return invoke(databaseService, "createDatabase", new Class<?>[]{configClass}, config);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to create SopLib database: " + t.getMessage());
            return null;
        }
    }

    public ItemStack createItem(String material, int amount, Object modelData, String name,
                                List<String> enchantments, List<String> lore, List<String> nbts) {
        if (!available) return null;
        try {
            Object itemUtils = invoke(sopLib, "getItemUtils", new Class<?>[0]);
            return (ItemStack) invoke(itemUtils, "createItem",
                    new Class<?>[]{String.class, int.class, Object.class, String.class, List.class, List.class, List.class},
                    material, amount, modelData, name, enchantments, lore, nbts);
        } catch (Throwable t) {
            return null;
        }
    }

    public ItemStack getHead(String textureValue, String displayName) {
        if (!available) return null;
        try {
            Object itemUtils = invoke(sopLib, "getItemUtils", new Class<?>[0]);
            return (ItemStack) invoke(itemUtils, "getHeadTexture",
                    new Class<?>[]{String.class, String.class}, textureValue, displayName);
        } catch (Throwable t) {
            return null;
        }
    }

    public void setCustomModelData(ItemStack item, Object... model) {
        if (!available || item == null) return;
        try {
            Object itemUtils = invoke(sopLib, "getItemUtils", new Class<?>[0]);
            Method method = itemUtils.getClass().getMethod("setCustomModelData", ItemStack.class, Object[].class);
            method.invoke(itemUtils, new Object[]{item, model});
        } catch (Throwable ignored) {
        }
    }

    public void setCustomItemKey(ItemStack item, String key, String fallback) {
        if (!available || item == null) return;
        try {
            Object itemUtils = invoke(sopLib, "getItemUtils", new Class<?>[0]);
            invoke(itemUtils, "setCustomItemKey",
                    new Class<?>[]{ItemStack.class, String.class, String.class}, item, key, fallback);
        } catch (Throwable ignored) {
        }
    }

    public String getCustomItemKey(ItemStack item) {
        if (!available || item == null) return null;
        try {
            Object itemUtils = invoke(sopLib, "getItemUtils", new Class<?>[0]);
            return (String) invoke(itemUtils, "getCustomItemKey", new Class<?>[]{ItemStack.class}, item);
        } catch (Throwable t) {
            return null;
        }
    }

    public String colorize(String text) {
        if (!available || text == null) {
            return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
        }
        try {
            Object textUtils = invoke(sopLib, "getTextUtils", new Class<?>[0]);
            return (String) invoke(textUtils, "color", new Class<?>[]{String.class}, text);
        } catch (Throwable t) {
            return ChatColor.translateAlternateColorCodes('&', text);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> colorize(List<String> lines) {
        if (!available || lines == null) {
            if (lines == null) return Collections.emptyList();
            List<String> result = new ArrayList<String>();
            for (String line : lines) {
                result.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            return result;
        }
        try {
            Object textUtils = invoke(sopLib, "getTextUtils", new Class<?>[0]);
            return (List<String>) invoke(textUtils, "color", new Class<?>[]{List.class}, lines);
        } catch (Throwable t) {
            List<String> result = new ArrayList<String>();
            for (String line : lines) {
                result.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            return result;
        }
    }

    public String serializeLocation(Location location) {
        if (!available || location == null) {
            return serializeLocationFallback(location);
        }
        try {
            Object util = invoke(sopLib, "getUtil", new Class<?>[0]);
            return (String) invoke(util, "getSerializedLocation", new Class<?>[]{Location.class}, location);
        } catch (Throwable t) {
            return serializeLocationFallback(location);
        }
    }

    public Location deserializeLocation(String serialized) {
        if (!available || serialized == null || serialized.isEmpty()) {
            return null;
        }
        try {
            Object util = invoke(sopLib, "getUtil", new Class<?>[0]);
            return (Location) invoke(util, "getDeserializedLocation", new Class<?>[]{String.class}, serialized);
        } catch (Throwable t) {
            return null;
        }
    }

    public void sendMessage(Player player, String message) {
        if (!available || player == null || message == null) {
            sendMessageFallback(player, message);
            return;
        }
        try {
            Object util = invoke(sopLib, "getUtil", new Class<?>[0]);
            invoke(util, "sendVersionedMessage", new Class<?>[]{Player.class, String.class}, player, message);
        } catch (Throwable t) {
            sendMessageFallback(player, message);
        }
    }

    public boolean canBuild(Player player, Location location) {
        if (!available) return true;
        try {
            Object protectionService = invoke(sopLib, "getProtectionService", new Class<?>[0]);
            Boolean result = (Boolean) invoke(protectionService, "canBuild",
                    new Class<?>[]{Player.class, Location.class}, player, location);
            return result == null || result;
        } catch (Throwable t) {
            return true;
        }
    }

    public void shutdown() {
        // SopLib manages its own lifecycle via its plugin.
    }

    private String serializeLocationFallback(Location location) {
        if (location == null || location.getWorld() == null) return "";
        return location.getWorld().getName() + ":" + location.getX() + ":" + location.getY() + ":" + location.getZ()
                + ":" + location.getYaw() + ":" + location.getPitch();
    }

    private void sendMessageFallback(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }
}
