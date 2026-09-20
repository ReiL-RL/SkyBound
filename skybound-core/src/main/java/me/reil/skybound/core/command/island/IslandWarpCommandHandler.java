package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandLogEntry;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class IslandWarpCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandWarpCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "warp":
                warp(player, args);
                return true;
            case "setwarp":
                setWarp(player, args);
                return true;
            case "delwarp":
                deleteWarp(player, args);
                return true;
            default:
                return false;
        }
    }

    private void warp(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }

        if (args.length < 2) {
            if (island.getWarps().isEmpty()) {
                context.lang().send(player, "warp.none");
                return;
            }
            context.lang().send(player, "warp.list-header");
            for (String warpName : island.getWarps().keySet()) {
                context.lang().send(player, "warp.list-entry", "{name}", warpName);
            }
            return;
        }

        Location location = island.getWarps().get(args[1]);
        if (location == null) {
            context.lang().send(player, "warp.not-found", "{name}", args[1]);
            return;
        }
        player.teleport(location);
        context.lang().send(player, "warp.teleported", "{name}", args[1]);
    }

    private void setWarp(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (args.length < 2) {
            context.lang().send(player, "usage",
                    "{usage}", "/is setwarp " + context.lang().get("arg.name"));
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.SET_WARP)) {
            return;
        }
        if (!island.isWithinBounds(player.getLocation())) {
            context.lang().send(player, "island.not-on-island");
            return;
        }

        String warpName = args[1];
        if (!isValidWarpName(warpName)) {
            context.lang().send(player, "warp.invalid-name");
            return;
        }
        int maxWarps = plugin.getCoreConfig().getMaxWarps();
        if (island.getWarps().size() >= maxWarps && !island.getWarps().containsKey(warpName)) {
            context.lang().send(player, "warp.max-reached", "{max}", String.valueOf(maxWarps));
            return;
        }

        island.setWarp(warpName, player.getLocation());
        plugin.getIslandManager().saveData();
        context.lang().send(player, "warp.set", "{name}", warpName);
        plugin.getIslandLogManager().log(island.getId(), player.getUniqueId(), player.getName(),
                IslandLogEntry.LogAction.WARP_SET, warpName);
    }

    private void deleteWarp(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (args.length < 2) {
            context.lang().send(player, "usage",
                    "{usage}", "/is delwarp " + context.lang().get("arg.name"));
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.DELETE_WARP)) {
            return;
        }

        String warpName = args[1];
        if (!isValidWarpName(warpName)) {
            context.lang().send(player, "warp.invalid-name");
            return;
        }
        island.removeWarp(warpName);
        plugin.getIslandManager().saveData();
        context.lang().send(player, "warp.removed", "{name}", warpName);
        plugin.getIslandLogManager().log(island.getId(), player.getUniqueId(), player.getName(),
                IslandLogEntry.LogAction.WARP_REMOVE, warpName);
    }

    private boolean isValidWarpName(String name) {
        return name != null
                && name.length() >= 1
                && name.length() <= 24
                && name.matches("[A-Za-z0-9_\\-]+");
    }
}
