package me.reil.skybound.core.command.island;

import me.reil.skybound.api.event.IslandRegenEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandImpl;
import me.reil.skybound.core.menu.IslandCreateMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public final class IslandLifecycleCommandHandler implements IslandSubCommandHandler {

    private static final String DEFAULT_SCHEMATIC = "desert.schem";

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;
    private final Set<String> regeneratingIslands = new HashSet<String>();

    public IslandLifecycleCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "create":
                create(player, args);
                return true;
            case "delete":
                delete(player);
                return true;
            case "regen":
                regenerate(player, args);
                return true;
            default:
                return false;
        }
    }

    private void create(Player player, String[] args) {
        if (context.getIsland(player) != null) {
            context.lang().send(player, "island.already-has");
            return;
        }

        if (args.length <= 1) {
            new IslandCreateMenu(player, plugin).open();
            return;
        }

        String fileName = normalizeSchematicName(args[1]);
        Island island = plugin.getIslandManager().createIsland(player, args[1]);
        if (island == null) {
            context.lang().send(player, "island.cannot-create");
            return;
        }

        plugin.getSchematicService().paste(fileName, island.getCenter());
        if (island instanceof IslandImpl) {
            ((IslandImpl) island).setSchematicName(fileName);
        }
        setSafeHomeAndTeleport(player, island);
        context.lang().send(player, "island.created");
    }

    private void delete(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (!island.getOwner().equals(player.getUniqueId())) {
            context.lang().send(player, "island.owner-only");
            return;
        }
        if (regeneratingIslands.contains(island.getId())) {
            context.lang().send(player, "island.regen-in-progress");
            return;
        }

        boolean needsConfirmation = plugin.getConfirmationManager()
                .requestConfirmation(player.getUniqueId(), "delete");
        if (needsConfirmation) {
            context.lang().send(player, "confirm.delete");
            return;
        }

        if (!plugin.getIslandManager().deleteIsland(island.getId())) {
            context.lang().send(player, "island.delete-failed");
            return;
        }
        plugin.getIslandChestManager().removeChest(island.getId());
        plugin.getPlayerShopManager().removeIslandShops(island.getId());
        plugin.getIslandBorderManager().removeIsland(island.getId());
        plugin.getIslandReviewManager().removeIsland(island.getId());
        plugin.getIslandAllianceManager().removeIsland(island.getId());
        plugin.getIslandPermissionManager().removeIsland(island.getId());
        plugin.getUpgradeManager().removeIsland(island.getId());
        plugin.getBoosterManager().removeIsland(island.getId());
        plugin.getPrestigeManager().removeIsland(island.getId());
        plugin.getPrestigeShopManager().removeIsland(island.getId());
    }

    private void regenerate(Player player, String[] args) {
        final Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (!island.getOwner().equals(player.getUniqueId())) {
            context.lang().send(player, "island.owner-only");
            return;
        }
        if (regeneratingIslands.contains(island.getId())) {
            context.lang().send(player, "island.regen-in-progress");
            return;
        }

        boolean needsConfirmation = plugin.getConfirmationManager()
                .requestConfirmation(player.getUniqueId(), "regen");
        if (needsConfirmation) {
            context.lang().send(player, "confirm.regen");
            return;
        }

        IslandRegenEvent event = new IslandRegenEvent(player, island);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            context.lang().send(player, "island.regen-failed");
            return;
        }

        String schematicName = args.length > 1 ? args[1] : getSavedSchematicName(island);
        final String fileName = normalizeSchematicName(schematicName);
        regeneratingIslands.add(island.getId());
        teleportPlayersOutOfIsland(island);
        boolean started = plugin.getIslandManager().regenerateIslandBatched(island.getId(), fileName, new Runnable() {
            @Override
            public void run() {
                try {
                    Island current = plugin.getIslandManager().getIsland(island.getId());
                    if (current == null) {
                        return;
                    }
                    plugin.getSchematicService().paste(fileName, current.getCenter());
                    setSafeHome(current);
                    Island playerIsland = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
                    if (player.isOnline() && playerIsland != null && playerIsland.getId().equals(current.getId())) {
                        player.teleport(current.getHome());
                        context.lang().send(player, "island.regenerated");
                    }
                } finally {
                    regeneratingIslands.remove(island.getId());
                }
            }
        });
        if (!started) {
            regeneratingIslands.remove(island.getId());
            context.lang().send(player, "island.regen-failed");
            return;
        }
        context.lang().send(player, "island.regen-started");
    }

    private void teleportPlayersOutOfIsland(Island island) {
        Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (island.isWithinBounds(online.getLocation())) {
                online.teleport(spawn);
            }
        }
    }

    private String getSavedSchematicName(Island island) {
        if (island instanceof IslandImpl) {
            String saved = ((IslandImpl) island).getSchematicName();
            if (saved != null && !saved.isEmpty()) {
                return saved;
            }
        }
        return DEFAULT_SCHEMATIC;
    }

    private String normalizeSchematicName(String schematicName) {
        if (schematicName.endsWith(".schem") || schematicName.endsWith(".schematic")) {
            return schematicName;
        }
        return schematicName + ".schem";
    }

    private void setSafeHomeAndTeleport(Player player, Island island) {
        setSafeHome(island);
        player.teleport(island.getHome());
    }

    private void setSafeHome(Island island) {
        Location home = island.getCenter().clone();
        home.setY(home.getWorld().getHighestBlockYAt(home.getBlockX(), home.getBlockZ()) + 1);
        home.setX(home.getBlockX() + 0.5);
        home.setZ(home.getBlockZ() + 0.5);
        island.setHome(home);
        plugin.getIslandManager().saveData();
    }
}
