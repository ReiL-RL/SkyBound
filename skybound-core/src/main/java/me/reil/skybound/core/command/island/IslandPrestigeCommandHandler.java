package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.PrestigeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class IslandPrestigeCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandPrestigeCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"prestige".equalsIgnoreCase(args[0])) {
            return false;
        }

        Island island = context.requireIsland(player);
        if (island == null) {
            return true;
        }
        if (!island.getOwner().equals(player.getUniqueId())) {
            context.lang().send(player, "island.owner-only");
            return true;
        }

        PrestigeManager prestigeManager = plugin.getPrestigeManager();
        if (!prestigeManager.canPrestige(island)) {
            sendCannotPrestige(player, island, prestigeManager);
            return true;
        }

        boolean needsConfirmation = plugin.getConfirmationManager()
                .requestConfirmation(player.getUniqueId(), "prestige");
        if (needsConfirmation) {
            int nextLevel = prestigeManager.getPrestigeLevel(island.getId()) + 1;
            context.lang().send(player, "prestige.info", "{level}", String.valueOf(nextLevel));
            context.lang().send(player, "confirm.prestige");
            return true;
        }

        int done = prestigeManager.prestige(player, island, 1);
        if (done <= 0) {
            sendCannotPrestige(player, island, prestigeManager);
            return true;
        }

        int newLevel = prestigeManager.getPrestigeLevel(island.getId());
        context.lang().send(player, "prestige.success");
        Bukkit.broadcastMessage(context.lang().get("prestige.broadcast",
                "{player}", player.getName(),
                "{level}", String.valueOf(newLevel)));
        return true;
    }

    private void sendCannotPrestige(Player player, Island island, PrestigeManager prestigeManager) {
        context.lang().send(player, "prestige.cannot-level",
                "{level}", String.valueOf(prestigeManager.getMinLevelToPrestige()),
                "{current}", String.valueOf(island.getLevel()));
    }
}
