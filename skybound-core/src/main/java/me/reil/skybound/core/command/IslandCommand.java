package me.reil.skybound.core.command;

import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.command.island.IslandAllianceCommandHandler;
import me.reil.skybound.core.command.island.IslandBorderCommandHandler;
import me.reil.skybound.core.command.island.IslandChatCommandHandler;
import me.reil.skybound.core.command.island.IslandCommandContext;
import me.reil.skybound.core.command.island.IslandCommandTabCompleter;
import me.reil.skybound.core.command.island.IslandGeneralCommandHandler;
import me.reil.skybound.core.command.island.IslandHelpCommandHandler;
import me.reil.skybound.core.command.island.IslandLifecycleCommandHandler;
import me.reil.skybound.core.command.island.IslandMenuCommandHandler;
import me.reil.skybound.core.command.island.IslandPrestigeCommandHandler;
import me.reil.skybound.core.command.island.IslandShopChestCommandHandler;
import me.reil.skybound.core.command.island.IslandSocialCommandHandler;
import me.reil.skybound.core.command.island.IslandSubCommandHandler;
import me.reil.skybound.core.command.island.IslandTaxCommandHandler;
import me.reil.skybound.core.command.island.IslandTeamCommandHandler;
import me.reil.skybound.core.command.island.IslandTradeCommandHandler;
import me.reil.skybound.core.command.island.IslandTransferCommandHandler;
import me.reil.skybound.core.command.island.IslandWarpCommandHandler;
import me.reil.skybound.core.lang.LangManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class IslandCommand implements CommandExecutor, TabCompleter {

    private final SkyBoundPlugin plugin;
    private final IslandMenuCommandHandler menuCommands;
    private final IslandCommandTabCompleter tabCompleter;
    private final List<IslandSubCommandHandler> subCommandHandlers;

    public IslandCommand(SkyBoundPlugin plugin) {
        this.plugin = plugin;
        IslandCommandContext context = new IslandCommandContext(plugin);
        this.menuCommands = new IslandMenuCommandHandler(context);
        this.tabCompleter = new IslandCommandTabCompleter();
        this.subCommandHandlers = Arrays.<IslandSubCommandHandler>asList(
                menuCommands,
                new IslandTeamCommandHandler(context),
                new IslandWarpCommandHandler(context),
                new IslandTransferCommandHandler(context),
                new IslandGeneralCommandHandler(context),
                new IslandSocialCommandHandler(context),
                new IslandLifecycleCommandHandler(context),
                new IslandAllianceCommandHandler(context),
                new IslandBorderCommandHandler(context),
                new IslandPrestigeCommandHandler(context),
                new IslandTradeCommandHandler(context),
                new IslandShopChestCommandHandler(context),
                new IslandChatCommandHandler(context),
                new IslandTaxCommandHandler(context),
                new IslandHelpCommandHandler(context));
    }

    private LangManager l() { return plugin.getLangManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(l().get("only-players")); return true; }
        Player p = (Player) sender;

        if (args.length == 0) {
            menuCommands.openDefault(p);
            return true;
        }

        for (IslandSubCommandHandler handler : subCommandHandlers) {
            if (handler.handle(p, args)) {
                return true;
            }
        }

        l().send(p, "unknown-command");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return tabCompleter.onTabComplete(sender, command, alias, args);
    }
}
