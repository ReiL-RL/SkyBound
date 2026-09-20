package me.reil.skybound.core.listener;

import me.reil.skybound.core.island.IslandAllianceManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AllianceChatListener implements Listener {

    private final JavaPlugin plugin;
    private final IslandAllianceManager allianceManager;

    public AllianceChatListener(JavaPlugin plugin, IslandAllianceManager allianceManager) {
        this.plugin = plugin;
        this.allianceManager = allianceManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!allianceManager.isChatToggled(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        final String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> allianceManager.sendAllianceMessage(event.getPlayer(), message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        allianceManager.disableChat(event.getPlayer().getUniqueId());
    }
}
