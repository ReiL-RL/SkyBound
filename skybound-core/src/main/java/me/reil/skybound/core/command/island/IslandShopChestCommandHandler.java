package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.PlayerShopManager;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

public final class IslandShopChestCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandShopChestCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"shopchest".equalsIgnoreCase(args[0])) {
            return false;
        }

        Island island = context.requireIsland(player);
        if (island == null) {
            return true;
        }

        if (args.length < 2) {
            context.lang().send(player, "shopchest.usage-set");
            context.lang().send(player, "shopchest.usage-remove");
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || !(target.getState() instanceof Chest)) {
            context.lang().send(player, "shopchest.look-at-chest");
            return true;
        }
        if (!island.isWithinBounds(target.getLocation())) {
            context.lang().send(player, "shopchest.not-on-island");
            return true;
        }
        if (context.lacksPermission(player, island, IslandPermission.MANAGE_SHOP)) {
            return true;
        }

        String subCommand = args[1].toLowerCase();
        if ("set".equals(subCommand)) {
            set(player, args, island, target);
        } else if ("remove".equals(subCommand)) {
            remove(player, target);
        } else {
            context.lang().send(player, "unknown-subcommand");
        }
        return true;
    }

    private void set(Player player, String[] args, Island island, Block target) {
        if (args.length < 3) {
            context.lang().send(player, "shopchest.price-required");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            context.lang().send(player, "number.price-required");
            return;
        }

        PlayerShopManager.Shop shop = plugin.getPlayerShopManager()
                .createShop(target.getLocation(), island.getId(), player.getUniqueId(), price);
        if (shop == null) {
            context.lang().send(player, "shopchest.already-shop");
            return;
        }
        context.lang().send(player, "shopchest.created", "{price}", String.valueOf(price));
    }

    private void remove(Player player, Block target) {
        if (plugin.getPlayerShopManager().removeShop(target.getLocation())) {
            context.lang().send(player, "shopchest.removed");
        } else {
            context.lang().send(player, "shopchest.not-shop");
        }
    }
}
