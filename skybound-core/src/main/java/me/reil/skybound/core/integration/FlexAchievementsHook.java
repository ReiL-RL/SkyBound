package me.reil.skybound.core.integration;

import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Hook into FlexAchievements plugin (optional).
 * Fires custom achievement events for things that don't have standard Bukkit events.
 *
 * Note: FlexAchievements already listens to SkyBound's Bukkit events
 * (IslandCreateEvent, IslandLevelUpEvent, etc.) via its SkyBoundBridge.
 * This hook is for additional custom events that SkyBound wants to fire
 * without a corresponding Bukkit event.
 */
public final class FlexAchievementsHook {

    private final SkyBoundPlugin plugin;
    private boolean available;
    private Object flexPlugin;
    private Method processMethod;
    private Method processAmountMethod;

    public FlexAchievementsHook(SkyBoundPlugin plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().isPluginEnabled("FlexAchievements");
        if (available) {
            try {
                flexPlugin = Bukkit.getPluginManager().getPlugin("FlexAchievements");
                processMethod = flexPlugin.getClass().getMethod("process", Player.class, String.class, Map.class);
                processAmountMethod = flexPlugin.getClass().getMethod("processAmount", Player.class, String.class, Map.class, int.class);
                plugin.getLogger().info("FlexAchievements detected - achievement hook active.");
            } catch (Exception e) {
                available = false;
                plugin.getLogger().warning("FlexAchievements found but API methods not available.");
            }
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Fire a custom event to FlexAchievements.
     *
     * @param player    the player
     * @param eventType the event type string (e.g. "SKYBOUND_PRESTIGE", "SKYBOUND_GENERATOR_COLLECT")
     * @param context   context map with additional data for conditions
     */
    public void fireEvent(Player player, String eventType, Map<String, Object> context) {
        if (!available || player == null) return;
        try {
            processMethod.invoke(flexPlugin, player, eventType, context);
        } catch (Exception ignored) {}
    }

    /**
     * Fire a custom event with amount to FlexAchievements.
     *
     * @param player    the player
     * @param eventType the event type string
     * @param context   context map
     * @param amount    progress amount
     */
    public void fireEventAmount(Player player, String eventType, Map<String, Object> context, int amount) {
        if (!available || player == null) return;
        try {
            processAmountMethod.invoke(flexPlugin, player, eventType, context, amount);
        } catch (Exception ignored) {}
    }
}
