package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join/quit for offline generator and data loading.
 */
public final class PlayerJoinListener implements Listener {

    private final SkyBoundPlugin plugin;
    private final IslandManager islandManager;

    public PlayerJoinListener(SkyBoundPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Give offline generator rewards
        plugin.getOfflineGeneratorManager().onMemberLogin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Check if all island members are now offline
        Island island = islandManager.getPlayerIsland(event.getPlayer().getUniqueId());
        if (island == null) return;

        // Schedule check for next tick (after player is fully disconnected)
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.getOfflineGeneratorManager().areAllMembersOffline(island)) {
                    plugin.getOfflineGeneratorManager().onAllMembersOffline(island.getId());
                }
            }
        }, 5L);
    }
}
