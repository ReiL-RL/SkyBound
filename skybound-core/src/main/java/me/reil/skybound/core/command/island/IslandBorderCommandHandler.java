package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandBorderManager;
import org.bukkit.entity.Player;

public final class IslandBorderCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandBorderCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"border".equalsIgnoreCase(args[0])) {
            return false;
        }

        Island island = context.requireIsland(player);
        if (island == null) {
            return true;
        }
        if (context.lacksPermission(player, island, IslandPermission.CHANGE_BORDER)) {
            return true;
        }

        IslandBorderManager borderManager = plugin.getIslandBorderManager();
        if (args.length < 2) {
            IslandBorderManager.BorderCycle state = borderManager.cycleState(island.getId());
            sendCycleMessage(player, state);
            reportBorder(player);
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "show":
            case "on":
                borderManager.setVisible(island.getId(), true);
                context.lang().send(player, "border.enabled");
                reportBorder(player);
                return true;
            case "hide":
            case "off":
                borderManager.setVisible(island.getId(), false);
                invalidateBorder(player);
                context.lang().send(player, "border.hidden");
                return true;
            case "toggle":
            case "cycle":
                IslandBorderManager.BorderCycle state = borderManager.cycleState(island.getId());
                sendCycleMessage(player, state);
                reportBorder(player);
                return true;
            default:
                context.lang().send(player, "border.usage");
                return true;
        }
    }

    private void sendCycleMessage(Player player, IslandBorderManager.BorderCycle state) {
        switch (state) {
            case RED:
                context.lang().send(player, "border.red");
                break;
            case GREEN:
                context.lang().send(player, "border.green");
                break;
            case BLUE:
                context.lang().send(player, "border.blue");
                break;
            default:
                context.lang().send(player, "border.off");
                break;
        }
    }

    private void reportBorder(Player player) {
        if (plugin.getBorderVisualListener() == null) {
            return;
        }

        String status = plugin.getBorderVisualListener().forceUpdate(player);
        if (status == null) {
            return;
        }
        if ("unsupported".equals(status)) {
            context.lang().send(player, "border.unsupported");
        } else if ("not-island-world".equals(status)) {
            context.lang().send(player, "border.not-island-world");
        } else if ("no-island-here".equals(status)) {
            context.lang().send(player, "border.no-island-here");
        }
    }

    private void invalidateBorder(Player player) {
        if (plugin.getBorderVisualListener() != null) {
            plugin.getBorderVisualListener().invalidate(player.getUniqueId());
        }
    }
}
