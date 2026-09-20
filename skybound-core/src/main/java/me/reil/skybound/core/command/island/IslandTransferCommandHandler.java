package me.reil.skybound.core.command.island;

import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class IslandTransferCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandTransferCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "giveisland":
                giveIsland(player, args);
                return true;
            case "sell":
                sell(player, args);
                return true;
            case "buy":
                buy(player);
                return true;
            default:
                return false;
        }
    }

    private void giveIsland(Player player, String[] args) {
        if (args.length < 2) {
            context.lang().send(player, "transfer.give-usage");
            return;
        }

        OfflinePlayer target = findKnownOfflinePlayer(args[1]);
        if (target == null) {
            context.lang().send(player, "player-not-found");
            return;
        }

        String error = plugin.getIslandTransferManager().transfer(player, target);
        if (error != null) {
            context.lang().send(player, error);
            return;
        }
        context.lang().send(player, "transfer.given", "{player}", getDisplayName(target, args[1]));
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null) {
            context.lang().send(onlineTarget, "transfer.received", "{player}", player.getName());
        }
    }

    private void sell(Player player, String[] args) {
        if (args.length < 3) {
            context.lang().send(player, "transfer.sell-usage");
            return;
        }

        OfflinePlayer target = findKnownOfflinePlayer(args[1]);
        if (target == null) {
            context.lang().send(player, "player-not-found");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            context.lang().send(player, "number.price-required");
            return;
        }

        String error = plugin.getIslandTransferManager().createSaleOffer(player, target, price);
        if (error != null) {
            context.lang().send(player, error);
        } else {
            context.lang().send(player, "transfer.sale-sent");
        }
    }

    private void buy(Player player) {
        String error = plugin.getIslandTransferManager().acceptOffer(player);
        if (error != null) {
            context.lang().send(player, error);
        } else {
            context.lang().send(player, "transfer.bought");
        }
    }

    private OfflinePlayer findKnownOfflinePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline == null || !offline.hasPlayedBefore()) {
            return null;
        }
        return offline;
    }

    private String getDisplayName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name != null ? name : fallback;
    }
}
