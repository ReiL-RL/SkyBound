package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class IslandTeamCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandTeamCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "invite":
                invite(player, args);
                return true;
            case "accept":
                accept(player);
                return true;
            case "deny":
                deny(player);
                return true;
            case "kick":
                kick(player, args);
                return true;
            case "ban":
                ban(player, args);
                return true;
            case "unban":
                unban(player, args);
                return true;
            case "leave":
                leave(player);
                return true;
            case "promote":
                promote(player, args);
                return true;
            case "demote":
                demote(player, args);
                return true;
            case "transfer":
                transfer(player, args);
                return true;
            case "trust":
                trust(player, args);
                return true;
            case "untrust":
                untrust(player, args);
                return true;
            case "coop":
                coop(player, args);
                return true;
            default:
                return false;
        }
    }

    private LangManager lang() {
        return context.lang();
    }

    private Player findOnlinePlayer(Player player, String[] args, int index) {
        if (args.length <= index) {
            lang().send(player, "usage", "{usage}", "/is " + args[0] + " " + lang().get("arg.player"));
            return null;
        }
        Player target = Bukkit.getPlayer(args[index]);
        if (target == null) {
            lang().send(player, "player-not-found");
        }
        return target;
    }

    private void invite(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        Player target = findOnlinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (context.lacksPermission(player, island, IslandPermission.INVITE)) {
            return;
        }
        if (plugin.getTeamManager().invite(island, player.getUniqueId(), target.getUniqueId())) {
            lang().send(player, "team.invited", "{player}", target.getName());
            lang().send(target, "team.invite-received", "{player}", player.getName());
        }
    }

    private void accept(Player player) {
        for (Island island : plugin.getIslandManager().getAllIslands()) {
            if (plugin.getTeamManager().hasPendingInvite(player.getUniqueId(), island.getId())) {
                plugin.getTeamManager().acceptInvite(player.getUniqueId(), island.getId());
                lang().send(player, "team.accepted");
                player.teleport(island.getHome());
                return;
            }
        }
        lang().send(player, "team.no-invites");
    }

    private void deny(Player player) {
        for (Island island : plugin.getIslandManager().getAllIslands()) {
            if (plugin.getTeamManager().hasPendingInvite(player.getUniqueId(), island.getId())) {
                plugin.getTeamManager().denyInvite(player.getUniqueId(), island.getId());
                lang().send(player, "team.denied");
                return;
            }
        }
        lang().send(player, "team.no-invites");
    }

    private void kick(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (plugin.getTeamManager().kick(island, player.getUniqueId(), target.getUniqueId())) {
            lang().send(player, "team.kicked", "{player}", getDisplayName(target, args[1]));
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                lang().send(onlineTarget, "team.kicked-target");
            }
        } else {
            lang().send(player, "team.cannot-kick");
        }
    }

    private void ban(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (plugin.getTeamManager().ban(island, player.getUniqueId(), target.getUniqueId())) {
            lang().send(player, "team.banned", "{player}", getDisplayName(target, args[1]));
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                lang().send(onlineTarget, "team.banned-target");
            }
        } else {
            lang().send(player, "team.cannot-ban");
        }
    }

    private void unban(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (plugin.getTeamManager().unban(island, player.getUniqueId(), target.getUniqueId())) {
            lang().send(player, "team.unbanned", "{player}", getDisplayName(target, args[1]));
        } else {
            lang().send(player, "team.cannot-unban");
        }
    }

    private void leave(Player player) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        if (island.getOwner().equals(player.getUniqueId())) {
            lang().send(player, "team.owner-cannot-leave");
            return;
        }
        island.removeMember(player.getUniqueId());
        plugin.getIslandManager().unregisterMember(player.getUniqueId(), island.getId());
        plugin.getIslandPermissionManager().removeMember(island.getId(), player.getUniqueId());
        plugin.getIslandManager().saveData();
        lang().send(player, "team.left");
    }

    private void promote(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (plugin.getTeamManager().promote(island, player.getUniqueId(), target.getUniqueId())) {
            String role = island.getMemberRole(target.getUniqueId()).name();
            lang().send(player, "team.promoted", "{player}", getDisplayName(target, args[1]), "{role}", role);
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                lang().send(onlineTarget, "team.promoted-target", "{role}", role);
            }
        } else {
            lang().send(player, "team.cannot-promote");
        }
    }

    private void demote(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (plugin.getTeamManager().demote(island, player.getUniqueId(), target.getUniqueId())) {
            lang().send(player, "team.demoted",
                    "{player}", getDisplayName(target, args[1]),
                    "{role}", island.getMemberRole(target.getUniqueId()).name());
        } else {
            lang().send(player, "team.cannot-demote");
        }
    }

    private void transfer(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        if (plugin.getTeamManager().transferOwnership(island, player.getUniqueId(), target.getUniqueId())) {
            lang().send(player, "team.transferred", "{player}", getDisplayName(target, args[1]));
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                lang().send(onlineTarget, "team.transferred-target");
            }
        } else {
            lang().send(player, "team.cannot-transfer");
        }
    }

    private void trust(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        boolean ok = plugin.getTeamManager().trust(island, player.getUniqueId(), target.getUniqueId());
        lang().send(player, ok ? "team.trusted" : "team.coop-cannot", "{player}", getDisplayName(target, args[1]));
    }

    private void untrust(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        boolean ok = plugin.getTeamManager().untrust(island, player.getUniqueId(), target.getUniqueId());
        lang().send(player, ok ? "team.untrusted" : "team.coop-cannot", "{player}", getDisplayName(target, args[1]));
    }

    private void coop(Player player, String[] args) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }
        OfflinePlayer target = findKnownOfflinePlayer(player, args, 1);
        if (target == null) {
            return;
        }
        boolean ok = plugin.getTeamManager().addCoop(island, player.getUniqueId(), target.getUniqueId());
        lang().send(player, ok ? "team.coop-added" : "team.coop-cannot", "{player}", getDisplayName(target, args[1]));
    }

    private OfflinePlayer findKnownOfflinePlayer(Player player, String[] args, int index) {
        if (args.length <= index) {
            lang().send(player, "usage", "{usage}", "/is " + args[0] + " " + lang().get("arg.player"));
            return null;
        }

        Player online = Bukkit.getPlayerExact(args[index]);
        if (online != null) {
            return online;
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[index]);
        if (offline == null || !offline.hasPlayedBefore()) {
            lang().send(player, "player-not-found");
            return null;
        }
        return offline;
    }

    private String getDisplayName(OfflinePlayer player, String fallback) {
        String name = player.getName();
        return name == null || name.isEmpty() ? fallback : name;
    }
}
