package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandProvider;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Island chat: when toggled, all chat messages from the player are
 * sent only to members of the same island.
 */
public final class IslandChatManager {

    private final JavaPlugin plugin;
    private final IslandProvider islandProvider;
    private final Set<UUID> chatToggled = new HashSet<UUID>();

    public IslandChatManager(JavaPlugin plugin, IslandProvider islandProvider) {
        this.plugin = plugin;
        this.islandProvider = islandProvider;
    }

    /** Toggle island chat for player. Returns new state. */
    public boolean toggle(UUID playerId) {
        if (chatToggled.contains(playerId)) {
            chatToggled.remove(playerId);
            return false;
        }
        chatToggled.add(playerId);
        return true;
    }

    public boolean isChatToggled(UUID playerId) {
        return chatToggled.contains(playerId);
    }

    public void disable(UUID playerId) {
        chatToggled.remove(playerId);
    }

    /**
     * Send a message to all online members of an island.
     */
    public void sendIslandMessage(Player from, String message) {
        Island island = islandProvider.getPlayerIsland(from.getUniqueId());
        if (island == null) {
            sendLang(from, "no-island");
            return;
        }
        String formatted = getLang("island.chat.format",
                "{player}", from.getName(),
                "{message}", message);
        for (UUID memberId : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
        plugin.getLogger().fine("[IslandChat] " + island.getId() + " "
                + from.getName() + ": " + message);
    }

    private void sendLang(Player player, String key, String... replacements) {
        if (plugin instanceof SkyBoundPlugin) {
            ((SkyBoundPlugin) plugin).getLangManager().send(player, key, replacements);
        }
    }

    private String getLang(String key, String... replacements) {
        if (plugin instanceof SkyBoundPlugin) {
            return ((SkyBoundPlugin) plugin).getLangManager().get(key, replacements);
        }
        return ChatColor.translateAlternateColorCodes('&', key);
    }
}
