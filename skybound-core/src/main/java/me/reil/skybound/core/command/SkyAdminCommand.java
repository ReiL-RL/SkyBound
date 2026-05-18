package me.reil.skybound.core.command;

import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Admin command: /sbadmin
 * Subcommands: reload, info, delete, setlevel, addons, recalculate
 */
public final class SkyAdminCommand implements CommandExecutor, TabCompleter {

    private final SkyBoundPlugin plugin;

    public SkyAdminCommand(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skybound.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6SkyBound Core v" + plugin.getDescription().getVersion());
            sender.sendMessage("§7Islands: " + plugin.getIslandManager().getIslandCount());
            sender.sendMessage("§7Addons: " + plugin.getAddonRegistry().getAddons().size());
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.getCoreConfig().reload();
                sender.sendMessage("§aConfiguration reloaded.");
                break;
            case "recalculate":
                plugin.getLeaderboardManager().recalculate();
                sender.sendMessage("§aLeaderboard recalculated.");
                break;
            case "addons":
                sender.sendMessage("§6=== Registered Addons ===");
                for (me.reil.skybound.api.addon.SkyBoundAddon addon : plugin.getAddonRegistry().getAddons()) {
                    sender.sendMessage("§7- " + addon.getAddonName() + " v" + addon.getAddonVersion());
                }
                break;
            default:
                sender.sendMessage("§cUnknown subcommand.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "info", "delete", "setlevel", "addons", "recalculate");
        }
        return Collections.emptyList();
    }
}
