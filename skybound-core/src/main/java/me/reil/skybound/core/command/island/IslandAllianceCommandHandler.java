package me.reil.skybound.core.command.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandAllianceManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class IslandAllianceCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandAllianceCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        String command = args[0].toLowerCase();
        if (!"alliance".equals(command) && !"ally".equals(command)) {
            return false;
        }

        if (args.length < 2) {
            context.lang().send(player, "alliance.usage");
            return true;
        }

        IslandAllianceManager allianceManager = plugin.getIslandAllianceManager();
        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "create":
                create(player, args, allianceManager);
                return true;
            case "invite":
                invite(player, args, allianceManager);
                return true;
            case "accept":
                accept(player, args, allianceManager);
                return true;
            case "leave":
                leave(player, allianceManager);
                return true;
            case "chat":
                chat(player, args, allianceManager);
                return true;
            case "info":
                info(player, allianceManager);
                return true;
            case "list":
                list(player, allianceManager);
                return true;
            default:
                context.lang().send(player, "alliance.unknown-subcommand");
                return true;
        }
    }

    private void create(Player player, String[] args, IslandAllianceManager allianceManager) {
        if (args.length < 3) {
            context.lang().send(player, "alliance.name-required");
            return;
        }

        StringBuilder name = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) {
                name.append(' ');
            }
            name.append(args[i]);
        }

        String allianceName = name.toString().trim();
        if (!isValidAllianceName(allianceName)) {
            context.lang().send(player, "alliance.invalid-name");
            return;
        }

        IslandAllianceManager.Alliance alliance = allianceManager.create(player, allianceName);
        if (alliance == null) {
            context.lang().send(player, "alliance.create-failed");
        } else {
            context.lang().send(player, "alliance.created",
                    "{name}", alliance.name,
                    "{id}", alliance.id);
        }
    }

    private void invite(Player player, String[] args, IslandAllianceManager allianceManager) {
        if (args.length < 3) {
            context.lang().send(player, "alliance.player-required");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            context.lang().send(player, "player-not-online");
            return;
        }

        Island targetIsland = plugin.getIslandManager().getPlayerIsland(target.getUniqueId());
        if (targetIsland == null) {
            context.lang().send(player, "alliance.target-no-island");
            return;
        }

        if (!allianceManager.invite(player, targetIsland.getId())) {
            context.lang().send(player, "alliance.invite-failed");
            return;
        }

        context.lang().send(player, "alliance.invite-sent");
        Island myIsland = plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
        IslandAllianceManager.Alliance alliance = myIsland == null ? null : allianceManager.getByIsland(myIsland.getId());
        if (alliance == null) {
            context.lang().send(player, "alliance.invite-failed");
            return;
        }

        context.lang().send(target, "alliance.invite-received", "{name}", alliance.name);
        sendAcceptButton(target, alliance.id);
    }

    private void sendAcceptButton(Player target, String allianceId) {
        try {
            TextComponent button = new TextComponent(context.lang().get("alliance.accept-button"));
            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/is alliance accept " + allianceId));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(context.lang().get("alliance.accept-hover"))));
            target.spigot().sendMessage(button);
        } catch (Throwable throwable) {
            context.lang().send(target, "alliance.accept-command", "{id}", allianceId);
        }
    }

    private void accept(Player player, String[] args, IslandAllianceManager allianceManager) {
        if (args.length < 3) {
            context.lang().send(player, "alliance.id-required");
            return;
        }

        if (allianceManager.accept(player, args[2])) {
            context.lang().send(player, "alliance.accepted");
        } else {
            context.lang().send(player, "alliance.accept-failed");
        }
    }

    private void leave(Player player, IslandAllianceManager allianceManager) {
        if (allianceManager.leave(player)) {
            context.lang().send(player, "alliance.left");
        } else {
            context.lang().send(player, "alliance.leave-failed");
        }
    }

    private void chat(Player player, String[] args, IslandAllianceManager allianceManager) {
        if (args.length > 2) {
            StringBuilder message = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) {
                    message.append(' ');
                }
                message.append(args[i]);
            }
            allianceManager.sendAllianceMessage(player, message.toString());
            return;
        }

        boolean enabled = allianceManager.toggleChat(player.getUniqueId());
        context.lang().send(player, enabled ? "alliance.chat-enabled" : "alliance.chat-disabled");
    }

    private void info(Player player, IslandAllianceManager allianceManager) {
        Island island = context.requireIsland(player);
        if (island == null) {
            return;
        }

        IslandAllianceManager.Alliance alliance = allianceManager.getByIsland(island.getId());
        if (alliance == null) {
            context.lang().send(player, "alliance.not-in");
            return;
        }

        context.lang().send(player, "alliance.info-header",
                "{name}", alliance.name,
                "{id}", alliance.id);
        context.lang().send(player, "alliance.info-leader", "{leader}", alliance.leaderIsland);
        context.lang().send(player, "alliance.info-members", "{count}", String.valueOf(alliance.islands.size()));
    }

    private void list(Player player, IslandAllianceManager allianceManager) {
        context.lang().send(player, "alliance.list-header");
        for (IslandAllianceManager.Alliance alliance : allianceManager.getAllAlliances()) {
            context.lang().send(player, "alliance.list-entry",
                    "{name}", alliance.name,
                    "{count}", String.valueOf(alliance.islands.size()),
                    "{id}", alliance.id);
        }
    }

    private boolean isValidAllianceName(String name) {
        return name != null
                && name.length() >= 3
                && name.length() <= 32
                && name.matches("[A-Za-zА-Яа-я0-9_\\- ]+");
    }
}
