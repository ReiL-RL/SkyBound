package me.reil.skybound.core.command.island;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class IslandCommandTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "home", "sethome", "invite", "accept", "deny", "kick", "leave",
            "promote", "demote", "transfer", "trust", "untrust", "coop", "lock", "name",
            "visit", "like", "trade", "warp", "setwarp", "delwarp", "warps", "top",
            "bank", "shop", "missions", "upgrades", "boosters", "members", "settings",
            "delete", "regen", "value", "level", "menu", "help", "biome", "autosell",
            "chest", "logs", "prestige", "chat", "border", "gift", "gifts", "review",
            "reviews", "stats", "alliance", "ally", "giveisland", "sell", "buy", "tax",
            "prestigeshop", "pshop", "shopchest", "deposit");

    private static final List<String> PLAYER_TARGET_SUBCOMMANDS = Arrays.asList(
            "invite", "kick", "promote", "demote", "transfer", "trust", "untrust",
            "coop", "visit", "giveisland", "sell");

    private static final List<String> ALLIANCE_SUBCOMMANDS = Arrays.asList(
            "create", "invite", "accept", "leave", "chat", "info", "list");

    private static final List<String> BORDER_SUBCOMMANDS = Arrays.asList(
            "show", "on", "hide", "off", "toggle", "cycle");

    private static final List<String> HELP_PAGES = Arrays.asList("1", "2", "3", "4", "admin");

    private static final List<String> SHOP_CHEST_SUBCOMMANDS = Arrays.asList("set", "remove");

    private static final List<String> TAX_SUBCOMMANDS = Arrays.asList("on", "enable", "off", "disable");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (PLAYER_TARGET_SUBCOMMANDS.contains(subCommand)) {
                return completeOnlinePlayers(args[1]);
            }
            if ("alliance".equals(subCommand) || "ally".equals(subCommand)) {
                return filterStartsWith(ALLIANCE_SUBCOMMANDS, args[1]);
            }
            if ("border".equals(subCommand)) {
                return filterStartsWith(BORDER_SUBCOMMANDS, args[1]);
            }
            if ("help".equals(subCommand)) {
                return filterStartsWith(HELP_PAGES, args[1]);
            }
            if ("shopchest".equals(subCommand)) {
                return filterStartsWith(SHOP_CHEST_SUBCOMMANDS, args[1]);
            }
            if ("tax".equals(subCommand) && sender.hasPermission("skybound.admin")) {
                return filterStartsWith(TAX_SUBCOMMANDS, args[1]);
            }
        }
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            String nestedSubCommand = args[1].toLowerCase();
            if (("alliance".equals(subCommand) || "ally".equals(subCommand))
                    && "invite".equals(nestedSubCommand)) {
                return completeOnlinePlayers(args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> completeOnlinePlayers(String rawPrefix) {
        List<String> names = new ArrayList<String>();
        String prefix = rawPrefix.toLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(prefix)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    private List<String> filterStartsWith(List<String> values, String rawPrefix) {
        String prefix = rawPrefix.toLowerCase();
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value.startsWith(prefix)) {
                result.add(value);
            }
        }
        return result;
    }
}
