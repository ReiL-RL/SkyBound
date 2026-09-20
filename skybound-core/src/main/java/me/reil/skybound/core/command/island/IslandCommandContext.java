package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.entity.Player;

public final class IslandCommandContext {

    private final SkyBoundPlugin plugin;

    public IslandCommandContext(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    public SkyBoundPlugin plugin() {
        return plugin;
    }

    public LangManager lang() {
        return plugin.getLangManager();
    }

    public Island getIsland(Player player) {
        return plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
    }

    public Island requireIsland(Player player) {
        Island island = getIsland(player);
        if (island == null) {
            lang().send(player, "island.no-island");
        }
        return island;
    }

    public boolean lacksPermission(Player player, Island island, IslandPermission permission) {
        if (island != null && plugin.getIslandPermissionManager().hasPermission(island, player.getUniqueId(), permission)) {
            return false;
        }
        lang().send(player, "no-permission");
        return true;
    }
}
