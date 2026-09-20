package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandImpl;
import me.reil.skybound.core.menu.IslandCreateMenu;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class IslandLifecycleCommandHandler implements IslandSubCommandHandler {

    private static final String DEFAULT_SCHEMATIC = "desert.schem";

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

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
        context.lang().send(player, "island.deleted");
    }

    private void regenerate(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (!island.getOwner().equals(player.getUniqueId())) {
            context.lang().send(player, "island.owner-only");
            return;
        }

        boolean needsConfirmation = plugin.getConfirmationManager()
                .requestConfirmation(player.getUniqueId(), "regen");
        if (needsConfirmation) {
            context.lang().send(player, "confirm.regen");
            return;
        }

        String schematicName = args.length > 1 ? args[1] : getSavedSchematicName(island);
        String fileName = normalizeSchematicName(schematicName);
        if (!plugin.getIslandManager().regenerateIsland(island.getId(), fileName)) {
            context.lang().send(player, "island.regen-failed");
            return;
        }
        plugin.getSchematicService().paste(fileName, island.getCenter());
        setSafeHomeAndTeleport(player, island);
        context.lang().send(player, "island.regenerated");
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
        Location home = island.getCenter().clone();
        home.setY(home.getWorld().getHighestBlockYAt(home.getBlockX(), home.getBlockZ()) + 1);
        home.setX(home.getBlockX() + 0.5);
        home.setZ(home.getBlockZ() + 0.5);
        island.setHome(home);
        player.teleport(island.getHome());
    }
}
