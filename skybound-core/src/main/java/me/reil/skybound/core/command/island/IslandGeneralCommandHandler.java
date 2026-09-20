package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandLogEntry;
import me.reil.skybound.core.menu.BiomeSelectMenu;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

public final class IslandGeneralCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandGeneralCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "home":
            case "go":
            case "h":
                home(player);
                return true;
            case "sethome":
                setHome(player);
                return true;
            case "lock":
                lock(player);
                return true;
            case "name":
                name(player, args);
                return true;
            case "value":
                value(player);
                return true;
            case "level":
                level(player);
                return true;
            case "autosell":
                autosell(player);
                return true;
            case "biome":
                biome(player, args);
                return true;
            case "logs":
                logs(player);
                return true;
            case "deposit":
                deposit(player);
                return true;
            default:
                return false;
        }
    }

    private void home(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        player.teleport(island.getHome());
        context.lang().send(player, "island.teleported-home");
    }

    private void setHome(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.SET_HOME)) {
            return;
        }
        if (!island.isWithinBounds(player.getLocation())) {
            context.lang().send(player, "island.not-on-island");
            return;
        }
        island.setHome(player.getLocation());
        context.lang().send(player, "island.home-set");
    }

    private void lock(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.LOCK_ISLAND)) {
            return;
        }
        island.setLocked(!island.isLocked());
        context.lang().send(player, island.isLocked() ? "island.locked" : "island.unlocked");
    }

    private void name(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.CHANGE_NAME)) {
            return;
        }
        if (args.length < 2) {
            context.lang().send(player, "usage",
                    "{usage}", "/is name " + context.lang().get("arg.name"));
            return;
        }

        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                name.append(' ');
            }
            name.append(args[i]);
        }

        String oldName = island.getName();
        String newName = name.toString();
        island.setName(newName);
        context.lang().send(player, "island.renamed", "{name}", newName);
        plugin.getIslandLogManager().log(island.getId(), player.getUniqueId(), player.getName(),
                IslandLogEntry.LogAction.ISLAND_RENAME, oldName + " → " + newName);
    }

    private void value(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        context.lang().send(player, "island.value", "{value}", String.format("%.0f", island.getValue()));
    }

    private void level(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        long next = (long) island.getLevel() * plugin.getCoreConfig().getXpPerLevel();
        context.lang().send(player, "island.level-info",
                "{level}", String.valueOf(island.getLevel()),
                "{xp}", String.valueOf(island.getExperience()),
                "{next}", String.valueOf(next));
    }

    private void autosell(Player player) {
        plugin.getAutosellListener().toggleAutosell(player.getUniqueId());
        context.lang().send(player,
                plugin.getAutosellListener().isAutosellEnabled(player.getUniqueId())
                        ? "autosell.enabled"
                        : "autosell.disabled");
    }

    private void biome(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.CHANGE_BIOME)) {
            return;
        }
        if (args.length < 2) {
            new BiomeSelectMenu(player, plugin, island).open();
            return;
        }

        Biome biome = plugin.getBiomeService().parseBiome(args[1]);
        if (biome == null) {
            context.lang().send(player, "biome.invalid", "{biome}", args[1]);
            return;
        }

        context.lang().send(player, "biome.changing");
        plugin.getBiomeService().changeBiome(island, biome);
        context.lang().send(player, "biome.changed", "{biome}", biome.name());
    }

    private void logs(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }

        java.util.List<IslandLogEntry> logs = plugin.getIslandLogManager().getLogs(island.getId(), 10);
        if (logs.isEmpty()) {
            context.lang().send(player, "logs.empty");
            return;
        }

        context.lang().send(player, "logs.header");
        for (IslandLogEntry entry : logs) {
            long ago = (System.currentTimeMillis() - entry.getTimestamp()) / 1000L;
            String time = ago < 60 ? ago + "s" : (ago < 3600 ? (ago / 60) + "m" : (ago / 3600) + "h");
            context.lang().send(player, "logs.entry",
                    "{time}", time,
                    "{player}", entry.getPlayerName(),
                    "{action}", entry.getAction().name().toLowerCase());
        }
    }

    private void deposit(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (plugin.getAddonRegistry().isRegistered("island-core")) {
            context.lang().send(player, "island-core.deposit-via-core");
            return;
        }
        plugin.getIslandValueMenu().open(player);
    }
}
