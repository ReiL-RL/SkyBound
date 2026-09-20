package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.menu.IslandReviewsMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class IslandSocialCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandSocialCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "visit":
                visit(player, args);
                return true;
            case "like":
                like(player);
                return true;
            case "gift":
                gift(player, args);
                return true;
            case "review":
                review(player, args);
                return true;
            case "reviews":
                reviews(player, args);
                return true;
            default:
                return false;
        }
    }

    private OfflinePlayer findKnownOfflinePlayer(Player player, String[] args, int index) {
        if (args.length <= index) {
            context.lang().send(player, "usage",
                    "{usage}", "/is " + args[0] + " " + context.lang().get("arg.player"));
            return null;
        }

        OfflinePlayer target = findKnownOfflinePlayer(args[index]);
        if (target == null) {
            context.lang().send(player, "player-not-found");
        }
        return target;
    }

    private void visit(Player player, String[] args) {
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }

        Island island = plugin.getIslandManager().getPlayerIsland(target.getUniqueId());
        if (island == null) {
            context.lang().send(player, "island.no-target-island");
            return;
        }
        if (island.isLocked()) {
            context.lang().send(player, "island.visit-locked");
            return;
        }
        if (plugin.getTeamManager().isBanned(island, player.getUniqueId())) {
            context.lang().send(player, "team.banned-from-island");
            return;
        }

        player.teleport(island.getHome());
        context.lang().send(player, "island.visited", "{player}", getDisplayName(target, args[1]));
    }

    private void like(Player player) {
        Island island = plugin.getIslandManager().getIslandAt(player.getLocation());
        if (island == null) {
            context.lang().send(player, "like.not-on-island");
            return;
        }
        if (island.getMembers().contains(player.getUniqueId())) {
            context.lang().send(player, "like.own-island");
            return;
        }
        if (plugin.getTeamManager().isBanned(island, player.getUniqueId())) {
            context.lang().send(player, "team.banned-from-island");
            return;
        }

        boolean liked = plugin.getVisitManager().like(player, island);
        if (liked) {
            context.lang().send(player, "like.success",
                    "{island}", island.getName(),
                    "{count}", String.valueOf(plugin.getVisitManager().getLikes(island)));
        } else {
            context.lang().send(player, "like.already-liked");
        }
    }

    private void gift(Player player, String[] args) {
        if (args.length < 2) {
            context.lang().send(player, "gift.usage");
            context.lang().send(player, "gift.remaining",
                    "{remaining}", String.valueOf(plugin.getIslandGiftManager().getRemainingToday(player.getUniqueId())),
                    "{limit}", String.valueOf(plugin.getIslandGiftManager().getDailyLimit()));
            return;
        }

        OfflinePlayer target = findKnownOfflinePlayer(args[1]);
        if (target == null) {
            context.lang().send(player, "player-not-found");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            context.lang().send(player, "gift.empty-hand");
            return;
        }

        String error = plugin.getIslandGiftManager().sendGift(player, target, hand.clone());
        if (error != null) {
            context.lang().send(player, error,
                    "{limit}", String.valueOf(plugin.getIslandGiftManager().getDailyLimit()));
            return;
        }

        player.getInventory().setItemInMainHand(null);
        context.lang().send(player, "gift.sent", "{player}", getDisplayName(target, args[1]));
    }

    private void review(Player player, String[] args) {
        if (args.length < 3) {
            context.lang().send(player, "review.usage");
            return;
        }

        Island target = findIslandByOwnerOrId(args[1]);
        if (target == null) {
            context.lang().send(player, "island.not-found");
            return;
        }
        if (target.getMembers().contains(player.getUniqueId())) {
            context.lang().send(player, "review.own-island");
            return;
        }
        if (plugin.getTeamManager().isBanned(target, player.getUniqueId())) {
            context.lang().send(player, "team.banned-from-island");
            return;
        }

        int stars;
        try {
            stars = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            context.lang().send(player, "review.invalid-stars");
            return;
        }
        if (stars < 1 || stars > 5) {
            context.lang().send(player, "review.invalid-stars");
            return;
        }

        StringBuilder comment = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) {
                comment.append(' ');
            }
            comment.append(args[i]);
        }
        if (comment.length() > 160) {
            context.lang().send(player, "review.too-long");
            return;
        }

        plugin.getIslandReviewManager().submitReview(target.getId(), player.getUniqueId(),
                player.getName(), stars, comment.toString());
        context.lang().send(player, "review.saved");
    }

    private void reviews(Player player, String[] args) {
        Island target;
        if (args.length >= 2) {
            target = findIslandByOwnerOrId(args[1]);
        } else {
            target = context.getIsland(player);
        }

        if (target == null) {
            context.lang().send(player, "island.not-found");
            return;
        }
        new IslandReviewsMenu(player, plugin, target).open();
    }

    private Island findIslandByOwnerOrId(String ownerOrId) {
        Player owner = Bukkit.getPlayerExact(ownerOrId);
        if (owner != null) {
            Island island = plugin.getIslandManager().getPlayerIsland(owner.getUniqueId());
            if (island != null) {
                return island;
            }
        }
        OfflinePlayer offlineOwner = findKnownOfflinePlayer(ownerOrId);
        if (offlineOwner != null) {
            Island island = plugin.getIslandManager().getPlayerIsland(offlineOwner.getUniqueId());
            if (island != null) {
                return island;
            }
        }
        return plugin.getIslandManager().getIsland(ownerOrId);
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
