package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandBorderManager;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.util.WorldBorderPacketUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Drives the real vanilla world border per-player (sent via packets).
 *
 * Because every island lives in the same world, we can't use the world's single
 * shared border. Instead each player is sent a personal border centered on the
 * island they are currently standing on. When they leave any island (or the
 * border is toggled off), the border is reset to the default huge size so no
 * wall is shown.
 *
 * Packets are only sent when the player's "border state" changes (island
 * changed, or visibility toggled) — not every tick.
 */
public final class BorderVisualListener {

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final IslandBorderManager borderManager;
    private final CoreConfig config;
    private BukkitTask task;

    /** playerId -> last border state we sent ("none" or "<islandId>"). */
    private final Map<UUID, String> lastState = new HashMap<UUID, String>();

    public BorderVisualListener(JavaPlugin plugin, IslandManager islandManager,
                                IslandBorderManager borderManager, CoreConfig config) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.borderManager = borderManager;
        this.config = config;
    }

    public void start() {
        if (!WorldBorderPacketUtil.isSupported(plugin)) {
            plugin.getLogger().warning("Island borders disabled: world border packets not supported here.");
            return;
        }
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, 20L); // once per second is enough — we only resend on change
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        // Best-effort reset for online players so a leftover border doesn't stick.
        for (Player player : Bukkit.getOnlinePlayers()) {
            WorldBorderPacketUtil.reset(plugin, player);
        }
        lastState.clear();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();

            String desired = "none";
            double cx = 0, cz = 0, diameter = 0;
            int colorMode = WorldBorderPacketUtil.COLOR_BLUE;

            if (player.getWorld().getName().startsWith(config.getIslandWorldName())) {
                Island island = islandManager.getIslandAt(player.getLocation());
                if (island != null && borderManager.isVisible(island.getId())) {
                    IslandBorderManager.BorderColor color = borderManager.getColor(island.getId());
                    colorMode = toColorMode(color);
                    desired = island.getId() + ":" + color.name();
                    Location center = island.getCenter();
                    cx = center.getX();
                    cz = center.getZ();
                    diameter = (island.getRadius() * 2.0D) + 1.0D;
                }
            }

            String prev = lastState.get(id);
            if (desired.equals(prev)) continue; // no change — don't resend

            if (desired.equals("none")) {
                WorldBorderPacketUtil.reset(plugin, player);
                lastState.put(id, "none");
            } else {
                WorldBorderPacketUtil.send(plugin, player, cx, cz, diameter, colorMode);
                lastState.put(id, desired);
            }
        }
    }

    private static int toColorMode(IslandBorderManager.BorderColor color) {
        switch (color) {
            case GREEN: return WorldBorderPacketUtil.COLOR_GREEN;
            case BLUE:  return WorldBorderPacketUtil.COLOR_BLUE;
            case RED:
            default:    return WorldBorderPacketUtil.COLOR_RED;
        }
    }

    /** Forget a player's tracked state (call on quit). */
    public void clearPlayer(UUID playerId) {
        lastState.remove(playerId);
    }

    /**
     * Immediately (re)send the border for one player, bypassing the once-per-second
     * tick. Returns a short status code used for in-game feedback / diagnostics:
     * "unsupported", "not-island-world", "no-island-here", "off", or "sent:<COLOR>".
     */
    public String forceUpdate(Player player) {
        if (!WorldBorderPacketUtil.isSupported(plugin)) {
            return "unsupported";
        }
        UUID id = player.getUniqueId();
        if (!player.getWorld().getName().startsWith(config.getIslandWorldName())) {
            return "not-island-world";
        }
        Island island = islandManager.getIslandAt(player.getLocation());
        if (island == null) {
            return "no-island-here";
        }
        if (!borderManager.isVisible(island.getId())) {
            WorldBorderPacketUtil.reset(plugin, player);
            lastState.put(id, "none");
            return "off";
        }
        IslandBorderManager.BorderColor color = borderManager.getColor(island.getId());
        int colorMode = toColorMode(color);
        Location center = island.getCenter();
        double diameter = (island.getRadius() * 2.0D) + 1.0D;
        WorldBorderPacketUtil.send(plugin, player, center.getX(), center.getZ(), diameter, colorMode);
        lastState.put(id, island.getId() + ":" + color.name());
        return "sent:" + color.name();
    }

    /**
     * Force-refresh a player's border on the next tick (e.g. after toggling
     * visibility or changing radius). Simply clears the cached state.
     */
    public void invalidate(UUID playerId) {
        lastState.remove(playerId);
    }
}
