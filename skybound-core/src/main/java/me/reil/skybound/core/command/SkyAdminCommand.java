package me.reil.skybound.core.command;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Admin command: /sbadmin (alias /skyadmin)
 * Subcommands: reload, info, addons, recalculate, tokens
 */
public final class SkyAdminCommand implements CommandExecutor, TabCompleter {

    private final SkyBoundPlugin plugin;

    public SkyAdminCommand(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skybound.admin")) {
            send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            send(sender, "admin.info.version", "{version}", plugin.getDescription().getVersion());
            send(sender, "admin.info.islands", "{count}", String.valueOf(plugin.getIslandManager().getIslandCount()));
            send(sender, "admin.info.addons", "{count}", String.valueOf(plugin.getAddonRegistry().getAddons().size()));
            send(sender, "admin.info.commands");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.getCoreConfig().reload();
                send(sender, "admin.reload.done");
                break;
            case "recalculate":
                plugin.getLeaderboardManager().recalculate();
                send(sender, "admin.recalculate.done");
                break;
            case "addons":
                send(sender, "admin.addons.header");
                for (me.reil.skybound.api.addon.SkyBoundAddon addon : plugin.getAddonRegistry().getAddons()) {
                    send(sender, "admin.addons.entry",
                            "{name}", addon.getAddonName(),
                            "{version}", addon.getAddonVersion());
                }
                break;
            case "tokens":
                handleTokens(sender, args);
                break;
            default:
                send(sender, "admin.unknown-subcommand");
                break;
        }
        return true;
    }

    /**
     * /sbadmin tokens <give|take|set> <player> <amount>
     * Manages a player's island prestige tokens.
     */
    private void handleTokens(CommandSender sender, String[] args) {
        if (args.length < 4) {
            send(sender, "admin.tokens.usage");
            return;
        }
        String action = args[1].toLowerCase();
        if (!action.equals("give") && !action.equals("take") && !action.equals("set")) {
            send(sender, "admin.tokens.invalid-action");
            return;
        }

        OfflinePlayer target = resolvePlayer(args[2]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore() && !target.isOnline())) {
            send(sender, "admin.tokens.player-not-found", "{player}", args[2]);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            send(sender, "admin.tokens.amount-number");
            return;
        }
        if (amount < 0) {
            send(sender, "admin.tokens.amount-negative");
            return;
        }

        Island island = plugin.getIslandManager().getPlayerIsland(target.getUniqueId());
        if (island == null) {
            send(sender, "admin.tokens.no-island", "{player}", target.getName());
            return;
        }

        me.reil.skybound.core.island.PrestigeShopManager psm = plugin.getPrestigeShopManager();
        String islandId = island.getId();
        switch (action) {
            case "give":
                psm.addTokens(islandId, amount);
                break;
            case "take":
                psm.removeTokens(islandId, amount);
                break;
            case "set":
                psm.setTokens(islandId, amount);
                break;
        }
        int now = psm.getTokens(islandId);
        send(sender, "admin.tokens.changed",
                "{player}", target.getName(),
                "{action}", action,
                "{amount}", String.valueOf(amount),
                "{balance}", String.valueOf(now));

        Player online = target.getPlayer();
        if (online != null) {
            plugin.getLangManager().send(online, "admin.tokens.changed-target", "{balance}", String.valueOf(now));
        }
    }

    private void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(plugin.getLangManager().get(key, replacements));
    }

    private OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        @SuppressWarnings("deprecation")
        OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("skybound.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return filter(Arrays.asList("reload", "recalculate", "addons", "tokens"), args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("tokens")) {
            if (args.length == 2) {
                return filter(Arrays.asList("give", "take", "set"), args[1]);
            }
            if (args.length == 3) {
                List<String> names = new ArrayList<String>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return filter(names, args[2]);
            }
            if (args.length == 4) {
                return filter(Arrays.asList("1", "5", "10", "50", "100"), args[3]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String prefix) {
        String low = prefix == null ? "" : prefix.toLowerCase();
        List<String> out = new ArrayList<String>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(low)) out.add(o);
        }
        return out;
    }
}
