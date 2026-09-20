package me.reil.skybound.core.listener;

import me.reil.skybound.core.island.IslandChatManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Intercepts chat from players who toggled island chat.
 * Routes those messages to the IslandChatManager (members only).
 */
public final class IslandChatListener implements Listener {

    private final JavaPlugin plugin;
    private final IslandChatManager chatManager;

    public IslandChatListener(JavaPlugin plugin, IslandChatManager chatManager) {
        this.plugin = plugin;
        this.chatManager = chatManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!chatManager.isChatToggled(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        // Use the player's message and dispatch on main thread for safety
        final String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> chatManager.sendIslandMessage(event.getPlayer(), message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatManager.disable(event.getPlayer().getUniqueId());
    }
}
