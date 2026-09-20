package me.reil.skybound.core.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Per-player world border helper.
 *
 * Every island lives in ONE world, so the world's single shared
 * {@link org.bukkit.WorldBorder} cannot represent each island. Bukkit added a
 * per-player world border API in 1.18.2 ({@code Bukkit.createWorldBorder()} +
 * {@code Player#setWorldBorder(WorldBorder)}), which is the clean, version-safe
 * way to do this on modern servers (1.18.2 → 1.21+).
 *
 * Because this plugin compiles against the 1.16.5 API (where those methods don't
 * exist yet), everything is invoked via reflection. If the running server is too
 * old to have the per-player API, it logs once and becomes a no-op instead of
 * crashing.
 *
 * Vanilla can only express three border colors, derived from the size state:
 * blue/aqua (stationary), green (growing) and red (shrinking). For green/red we
 * start a barely-moving size transition (1 block over ~31 years) so the wall
 * stays put but the client still tints it.
 */
public final class WorldBorderPacketUtil {

    /** Color modes — the vanilla world border only supports these three states. */
    public static final int COLOR_BLUE = 0;  // stationary (aqua/blue)
    public static final int COLOR_GREEN = 1; // growing
    public static final int COLOR_RED = 2;   // shrinking

    private static final double COLOR_DELTA = 1.0D;
    private static final long COLOR_TIME_SECONDS = 1_000_000_000L; // ~31 years; imperceptible movement

    private static boolean initialised = false;
    private static boolean ok = false;
    private static boolean loggedSendError = false;

    // Reflected Bukkit per-player WorldBorder API (1.18.2+)
    private static Method createWorldBorder;    // Bukkit.createWorldBorder()
    private static Method setWorldBorder;       // Player.setWorldBorder(WorldBorder)
    private static Method wbSetCenter;          // WorldBorder.setCenter(double,double)
    private static Method wbSetSize;            // WorldBorder.setSize(double)
    private static Method wbSetSizeOverTime;    // WorldBorder.setSize(double,long)
    private static Method wbSetWarningDistance; // WorldBorder.setWarningDistance(int)
    private static Method wbSetWarningTime;     // WorldBorder.setWarningTime(int)

    private WorldBorderPacketUtil() {}

    private static synchronized void init(Plugin plugin) {
        if (initialised) return;
        initialised = true;
        try {
            Class<?> worldBorderClass = Class.forName("org.bukkit.WorldBorder");
            createWorldBorder = Bukkit.class.getMethod("createWorldBorder");
            setWorldBorder = Player.class.getMethod("setWorldBorder", worldBorderClass);
            wbSetCenter = worldBorderClass.getMethod("setCenter", double.class, double.class);
            wbSetSize = worldBorderClass.getMethod("setSize", double.class);
            try {
                wbSetSizeOverTime = worldBorderClass.getMethod("setSize", double.class, long.class);
            } catch (NoSuchMethodException ignored) {
                wbSetSizeOverTime = null; // colors fall back to stationary blue
            }
            wbSetWarningDistance = worldBorderClass.getMethod("setWarningDistance", int.class);
            wbSetWarningTime = worldBorderClass.getMethod("setWarningTime", int.class);
            ok = true;
            plugin.getLogger().info("Per-player world border enabled (Bukkit API).");
        } catch (Throwable t) {
            ok = false;
            plugin.getLogger().warning("Per-player world border unavailable on this server: " + t
                    + " — island borders disabled (needs 1.18.2+).");
        }
    }

    /** Whether the per-player world border API is available on this server. */
    public static boolean isSupported(Plugin plugin) {
        init(plugin);
        return ok;
    }

    /**
     * Show a per-player world border centered at (centerX, centerZ) with the given
     * diameter (in blocks). Only this player sees it. Stationary (blue) color.
     */
    public static void send(Plugin plugin, Player player, double centerX, double centerZ, double diameter) {
        send(plugin, player, centerX, centerZ, diameter, COLOR_BLUE);
    }

    /**
     * Show a per-player world border with a chosen color mode.
     */
    public static void send(Plugin plugin, Player player, double centerX, double centerZ,
                            double diameter, int colorMode) {
        init(plugin);
        if (!ok) return;
        try {
            Object wb = createWorldBorder.invoke(null);
            wbSetCenter.invoke(wb, centerX, centerZ);
            wbSetWarningTime.invoke(wb, 0);
            wbSetWarningDistance.invoke(wb, 0);
            wbSetSize.invoke(wb, diameter);

            if (colorMode == COLOR_GREEN && wbSetSizeOverTime != null) {
                wbSetSizeOverTime.invoke(wb, diameter + COLOR_DELTA, COLOR_TIME_SECONDS);
            } else if (colorMode == COLOR_RED && wbSetSizeOverTime != null) {
                wbSetSizeOverTime.invoke(wb, Math.max(1.0D, diameter - COLOR_DELTA), COLOR_TIME_SECONDS);
            }

            setWorldBorder.invoke(player, wb);
        } catch (Throwable t) {
            if (!loggedSendError) {
                loggedSendError = true;
                plugin.getLogger().warning("Failed to apply world border: " + t);
            }
        }
    }

    /**
     * Reset the player's view to the world's real border (removes the per-player one).
     */
    public static void reset(Plugin plugin, Player player) {
        init(plugin);
        if (!ok) return;
        try {
            setWorldBorder.invoke(player, (Object) null);
        } catch (Throwable t) {
            // ignore — best-effort reset
        }
    }
}
