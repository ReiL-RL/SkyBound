package me.reil.skybound.core.command.island;

import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandTaxManager;
import org.bukkit.entity.Player;

public final class IslandTaxCommandHandler implements IslandSubCommandHandler {

    private final SkyBoundPlugin plugin;
    private final IslandCommandContext context;

    public IslandTaxCommandHandler(IslandCommandContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    @Override
    public boolean handle(Player player, String[] args) {
        if (!"tax".equalsIgnoreCase(args[0])) {
            return false;
        }

        if (!player.hasPermission("skybound.admin")) {
            context.lang().send(player, "admin-only");
            return true;
        }

        IslandTaxManager taxManager = plugin.getIslandTaxManager();
        if (args.length < 2) {
            context.lang().send(player, "tax.status",
                    "{status}", context.lang().get(taxManager.isEnabled() ? "state.on" : "state.off"));
            context.lang().send(player, "tax.info",
                    "{interval}", String.valueOf(taxManager.getIntervalSeconds()),
                    "{flat}", String.valueOf(taxManager.getFlatTax()),
                    "{percent}", String.valueOf(taxManager.getPercentTax()));
            context.lang().send(player, "tax.usage");
            return true;
        }

        String subCommand = args[1].toLowerCase();
        if ("on".equals(subCommand) || "enable".equals(subCommand)) {
            taxManager.setEnabled(true);
            taxManager.start();
            context.lang().send(player, "tax.enabled");
        } else if ("off".equals(subCommand) || "disable".equals(subCommand)) {
            taxManager.setEnabled(false);
            taxManager.stop();
            context.lang().send(player, "tax.disabled");
        } else {
            context.lang().send(player, "unknown-subcommand");
        }
        return true;
    }
}
