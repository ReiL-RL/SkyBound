package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.entity.Player;

public final class IslandChatCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandChatCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"chat".equalsIgnoreCase(args[0])) {
            return false;
        }

        Island island = context.requireIsland(player);
        if (island == null) {
            return true;
        }

        if (args.length > 1) {
            StringBuilder message = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) {
                    message.append(' ');
                }
                message.append(args[i]);
            }
            plugin.getIslandChatManager().sendIslandMessage(player, message.toString());
            return true;
        }

        boolean enabled = plugin.getIslandChatManager().toggle(player.getUniqueId());
        context.lang().send(player, enabled ? "island-chat.enabled" : "island-chat.disabled");
        return true;
    }
}
