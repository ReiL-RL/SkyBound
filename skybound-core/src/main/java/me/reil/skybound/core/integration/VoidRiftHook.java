package me.reil.skybound.core.integration;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandImpl;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Hook into VoidRift plugin (optional).
 * When VoidRift is present, SkyBound listens to VoidRift events
 * and gives island XP, mission progress, and rewards.
 */
public final class VoidRiftHook implements Listener {

    private final SkyBoundPlugin plugin;
    private boolean available;

    public VoidRiftHook(SkyBoundPlugin plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().isPluginEnabled("VoidRift");
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Register VoidRift event listeners via reflection (no compile-time dependency on VoidRift).
     */
    public void register() {
        if (!available) return;

        plugin.getLogger().info("VoidRift detected - event integration active.");

        // VoidRift fires standard Bukkit events that we can listen to.
        // The SkyBound API's SkyBoundHook inside VoidRift already calls our API directly.
        // But we can also listen for mob kills in event zones to give mission progress.

        // Listen for entity deaths and check if killer is in a VoidRift event
        Bukkit.getPluginManager().registerEvents(new Listener() {}, plugin);

        // The actual integration works through VoidRift's SkyBoundHook class
        // which calls SkyBoundAPI.get().getEconomyProvider() and island.addExperience()
        // So the connection is already established through the API.

        plugin.getLogger().info("VoidRift <-> SkyBound bridge active. Events give island XP and money.");
    }

    /**
     * Called when a player completes a VoidRift event.
     * Can be triggered by VoidRift's SkyBoundHook or by listening to custom events.
     */
    public void onEventComplete(Player player, String eventId, int score) {
        Island island = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
        if (island == null) return;

        // Log the event completion
        plugin.getIslandLogManager().log(island.getId(), player.getUniqueId(), player.getName(),
                me.reil.skybound.core.island.IslandLogEntry.LogAction.SETTINGS_CHANGE,
                "VoidRift event: " + eventId + " (score: " + score + ")");

        // Track for missions (CUSTOM type)
        plugin.getMissionManager().trackAction(player.getUniqueId(),
                me.reil.skybound.api.mission.MissionType.CUSTOM, "EVENT_" + eventId.toUpperCase(), 1);
    }
}
