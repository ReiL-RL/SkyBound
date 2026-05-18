package me.reil.skybound.core.command;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.lang.LangManager;
import me.reil.skybound.core.menu.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class IslandCommand implements CommandExecutor, TabCompleter {

    private final SkyBoundPlugin plugin;

    public IslandCommand(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    private LangManager l() { return plugin.getLangManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Only players."); return true; }
        Player p = (Player) sender;

        if (args.length == 0) {
            Island is = plugin.getIslandManager().getPlayerIsland(p.getUniqueId());
            if (is == null) { new IslandCreateMenu(p, plugin).open(); }
            else { new IslandMainMenu(p, plugin, is).open(); }
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create": cmdCreate(p, args); break;
            case "home": case "go": case "h": cmdHome(p); break;
            case "sethome": cmdSetHome(p); break;
            case "invite": cmdInvite(p, args); break;
            case "accept": cmdAccept(p); break;
            case "deny": cmdDeny(p); break;
            case "kick": cmdKick(p, args); break;
            case "leave": cmdLeave(p); break;
            case "promote": cmdPromote(p, args); break;
            case "demote": cmdDemote(p, args); break;
            case "transfer": cmdTransfer(p, args); break;
            case "trust": cmdTrust(p, args); break;
            case "untrust": cmdUntrust(p, args); break;
            case "coop": cmdCoop(p, args); break;
            case "lock": cmdLock(p); break;
            case "name": cmdName(p, args); break;
            case "visit": cmdVisit(p, args); break;
            case "like": cmdLike(p); break;
            case "trade": cmdTrade(p, args); break;
            case "warp": cmdWarp(p, args); break;
            case "setwarp": cmdSetWarp(p, args); break;
            case "delwarp": cmdDelWarp(p, args); break;
            case "warps": cmdWarps(p); break;
            case "top": new TopIslandsMenu(p, plugin).open(); break;
            case "bank": cmdBank(p); break;
            case "shop": new ShopCategoryMenu(p, plugin).open(); break;
            case "missions": new MissionCategoryMenu(p, plugin).open(); break;
            case "upgrades": cmdUpgrades(p); break;
            case "boosters": cmdBoosters(p); break;
            case "members": cmdMembers(p); break;
            case "settings": cmdSettings(p); break;
            case "delete": cmdDelete(p); break;
            case "regen": cmdRegen(p, args); break;
            case "value": cmdValue(p); break;
            case "level": cmdLevel(p); break;
            case "menu": cmdMenu(p); break;
            case "biome": cmdBiome(p, args); break;
            case "autosell": cmdAutosell(p); break;
            case "chest": cmdChest(p); break;
            case "logs": cmdLogs(p); break;
            case "prestige": cmdPrestige(p); break;
            case "help": cmdHelp(p); break;
            default: l().send(p, "unknown-command"); break;
        }
        return true;
    }

    private Island getIsland(Player p) { return plugin.getIslandManager().getPlayerIsland(p.getUniqueId()); }
    private boolean noIsland(Player p) { Island i = getIsland(p); if (i == null) l().send(p, "island.no-island"); return i == null; }
    private Player findPlayer(Player p, String[] args, int idx) {
        if (args.length <= idx) { l().send(p, "usage", "{usage}", "/is " + args[0] + " <\u0438\u0433\u0440\u043e\u043a>"); return null; }
        Player t = Bukkit.getPlayer(args[idx]);
        if (t == null) l().send(p, "player-not-found");
        return t;
    }

    private void cmdCreate(Player p, String[] args) {
        if (getIsland(p) != null) { l().send(p, "island.already-has"); return; }
        if (args.length > 1) {
            Island is = plugin.getIslandManager().createIsland(p, args[1]);
            if (is != null) { plugin.getSchematicService().paste(args[1] + ".schem", is.getCenter()); p.teleport(is.getHome()); l().send(p, "island.created"); }
            else l().send(p, "island.cannot-create");
        } else { new IslandCreateMenu(p, plugin).open(); }
    }
    private void cmdHome(Player p) { if (noIsland(p)) return; p.teleport(getIsland(p).getHome()); l().send(p, "island.teleported-home"); }
    private void cmdSetHome(Player p) { if (noIsland(p)) return; Island is = getIsland(p); if (!is.isWithinBounds(p.getLocation())) { l().send(p, "island.not-on-island"); return; } is.setHome(p.getLocation()); l().send(p, "island.home-set"); }
    private void cmdInvite(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; Island is = getIsland(p); if (!is.getMemberRole(p.getUniqueId()).isAtLeast(IslandRole.MODERATOR)) { l().send(p, "no-permission"); return; } boolean ok = plugin.getTeamManager().invite(is, p.getUniqueId(), t.getUniqueId()); if (ok) { l().send(p, "team.invited", "{player}", t.getName()); l().send(t, "team.invite-received", "{player}", p.getName()); } }
    private void cmdAccept(Player p) { for (Island is : plugin.getIslandManager().getAllIslands()) { if (plugin.getTeamManager().hasPendingInvite(p.getUniqueId(), is.getId())) { plugin.getTeamManager().acceptInvite(p.getUniqueId(), is.getId()); l().send(p, "team.accepted"); p.teleport(is.getHome()); return; } } l().send(p, "team.no-invites"); }
    private void cmdDeny(Player p) { for (Island is : plugin.getIslandManager().getAllIslands()) { if (plugin.getTeamManager().hasPendingInvite(p.getUniqueId(), is.getId())) { plugin.getTeamManager().denyInvite(p.getUniqueId(), is.getId()); l().send(p, "team.denied"); return; } } l().send(p, "team.no-invites"); }
    private void cmdKick(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; Island is = getIsland(p); boolean ok = plugin.getTeamManager().kick(is, p.getUniqueId(), t.getUniqueId()); if (ok) { plugin.getIslandManager().unregisterMember(t.getUniqueId()); l().send(p, "team.kicked", "{player}", t.getName()); l().send(t, "team.kicked-target"); } else l().send(p, "team.cannot-kick"); }
    private void cmdLeave(Player p) { if (noIsland(p)) return; Island is = getIsland(p); if (is.getOwner().equals(p.getUniqueId())) { l().send(p, "team.owner-cannot-leave"); return; } is.removeMember(p.getUniqueId()); plugin.getIslandManager().unregisterMember(p.getUniqueId()); l().send(p, "team.left"); }
    private void cmdPromote(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; Island is = getIsland(p); boolean ok = plugin.getTeamManager().promote(is, p.getUniqueId(), t.getUniqueId()); if (ok) { l().send(p, "team.promoted", "{player}", t.getName(), "{role}", is.getMemberRole(t.getUniqueId()).name()); l().send(t, "team.promoted-target", "{role}", is.getMemberRole(t.getUniqueId()).name()); } else l().send(p, "team.cannot-promote"); }
    private void cmdDemote(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; Island is = getIsland(p); boolean ok = plugin.getTeamManager().demote(is, p.getUniqueId(), t.getUniqueId()); if (ok) l().send(p, "team.demoted", "{player}", t.getName(), "{role}", is.getMemberRole(t.getUniqueId()).name()); else l().send(p, "team.cannot-demote"); }
    private void cmdTransfer(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; Island is = getIsland(p); boolean ok = plugin.getTeamManager().transferOwnership(is, p.getUniqueId(), t.getUniqueId()); if (ok) { l().send(p, "team.transferred", "{player}", t.getName()); l().send(t, "team.transferred-target"); } else l().send(p, "team.cannot-transfer"); }
    private void cmdTrust(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; boolean ok = plugin.getTeamManager().trust(getIsland(p), p.getUniqueId(), t.getUniqueId()); l().send(p, ok ? "team.trusted" : "team.coop-cannot", "{player}", t.getName()); }
    private void cmdUntrust(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; boolean ok = plugin.getTeamManager().untrust(getIsland(p), p.getUniqueId(), t.getUniqueId()); l().send(p, ok ? "team.untrusted" : "team.coop-cannot", "{player}", t.getName()); }
    private void cmdCoop(Player p, String[] args) { if (noIsland(p)) return; Player t = findPlayer(p, args, 1); if (t == null) return; boolean ok = plugin.getTeamManager().addCoop(getIsland(p), p.getUniqueId(), t.getUniqueId()); l().send(p, ok ? "team.coop-added" : "team.coop-cannot", "{player}", t.getName()); }
    private void cmdLock(Player p) { if (noIsland(p)) return; Island is = getIsland(p); is.setLocked(!is.isLocked()); l().send(p, is.isLocked() ? "island.locked" : "island.unlocked"); }
    private void cmdName(Player p, String[] args) { if (noIsland(p)) return; if (args.length < 2) { l().send(p, "usage", "{usage}", "/is name <\u0438\u043c\u044f>"); return; } StringBuilder n = new StringBuilder(); for (int i = 1; i < args.length; i++) { if (i > 1) n.append(" "); n.append(args[i]); } getIsland(p).setName(n.toString()); l().send(p, "island.renamed", "{name}", n.toString()); }
    private void cmdVisit(Player p, String[] args) { Player t = findPlayer(p, args, 1); if (t == null) return; Island is = plugin.getIslandManager().getPlayerIsland(t.getUniqueId()); if (is == null) { l().send(p, "island.no-target-island"); return; } if (is.isLocked()) { l().send(p, "island.visit-locked"); return; } p.teleport(is.getHome()); l().send(p, "island.visited", "{player}", t.getName()); }
    private void cmdLike(Player p) {
        me.reil.skybound.api.island.Island island = plugin.getIslandManager().getIslandAt(p.getLocation());
        if (island == null) { p.sendMessage("§cВы не на острове."); return; }
        if (island.getMembers().contains(p.getUniqueId())) { p.sendMessage("§cНельзя лайкнуть свой остров."); return; }
        boolean liked = plugin.getVisitManager().like(p, island);
        if (liked) { p.sendMessage("§a❤ Вы поставили лайк острову §e" + island.getName() + "§a! (Всего: " + plugin.getVisitManager().getLikes(island) + ")"); }
        else { p.sendMessage("§cВы уже лайкали этот остров сегодня."); }
    }
    private void cmdTrade(Player p, String[] args) {
        if (args.length >= 3 && "sell".equalsIgnoreCase(args[1])) {
            // /is trade sell <price>
            org.bukkit.inventory.ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == org.bukkit.Material.AIR) { p.sendMessage("§cВозьмите предмет в руку."); return; }
            double price;
            try { price = Double.parseDouble(args[2]); } catch (NumberFormatException e) { p.sendMessage("§cУкажите цену числом."); return; }
            if (price <= 0) { p.sendMessage("§cЦена должна быть больше 0."); return; }
            me.reil.skybound.api.trade.TradeOffer offer = plugin.getTradeManager().createOffer(p, hand, price);
            if (offer != null) { p.sendMessage("§aПредложение создано! Цена: §e" + String.format("%.0f", price) + " монет"); }
            else { p.sendMessage("§cНе удалось создать предложение."); }
            return;
        }
        new me.reil.skybound.core.menu.TradeMenu(p, plugin).open();
    }
    private void cmdWarps(Player p) { if (noIsland(p)) return; new WarpsMenu(p, plugin, getIsland(p)).open(); }
    private void cmdWarp(Player p, String[] args) { if (noIsland(p)) return; Island is = getIsland(p); if (args.length < 2) { if (is.getWarps().isEmpty()) { l().send(p, "warp.none"); } else { l().send(p, "warp.list-header"); for (String w : is.getWarps().keySet()) l().send(p, "warp.list-entry", "{name}", w); } return; } Location loc = is.getWarps().get(args[1]); if (loc == null) { l().send(p, "warp.not-found", "{name}", args[1]); return; } p.teleport(loc); l().send(p, "warp.teleported", "{name}", args[1]); }
    private void cmdSetWarp(Player p, String[] args) { if (noIsland(p)) return; if (args.length < 2) { l().send(p, "usage", "{usage}", "/is setwarp <\u0438\u043c\u044f>"); return; } Island is = getIsland(p); if (!is.isWithinBounds(p.getLocation())) { l().send(p, "island.not-on-island"); return; } int max = plugin.getCoreConfig().getMaxWarps(); if (is.getWarps().size() >= max && !is.getWarps().containsKey(args[1])) { l().send(p, "warp.max-reached", "{max}", String.valueOf(max)); return; } is.setWarp(args[1], p.getLocation()); l().send(p, "warp.set", "{name}", args[1]); }
    private void cmdDelWarp(Player p, String[] args) { if (noIsland(p)) return; if (args.length < 2) { l().send(p, "usage", "{usage}", "/is delwarp <\u0438\u043c\u044f>"); return; } getIsland(p).removeWarp(args[1]); l().send(p, "warp.removed", "{name}", args[1]); }
    private void cmdBank(Player p) { if (noIsland(p)) return; new BankMenu(p, plugin, getIsland(p)).open(); }
    private void cmdUpgrades(Player p) { if (noIsland(p)) return; new UpgradesMenu(p, plugin, getIsland(p)).open(); }
    private void cmdBoosters(Player p) { if (noIsland(p)) return; new BoostersMenu(p, plugin, getIsland(p)).open(); }
    private void cmdMembers(Player p) { if (noIsland(p)) return; new IslandMembersMenu(p, plugin, getIsland(p)).open(); }
    private void cmdSettings(Player p) { if (noIsland(p)) return; new IslandSettingsMenu(p, plugin, getIsland(p)).open(); }
    private void cmdMenu(Player p) { if (noIsland(p)) return; new IslandMainMenu(p, plugin, getIsland(p)).open(); }
    private void cmdValue(Player p) { if (noIsland(p)) return; l().send(p, "island.value", "{value}", String.format("%.0f", getIsland(p).getValue())); }
    private void cmdLevel(Player p) { if (noIsland(p)) return; Island is = getIsland(p); long next = (long) is.getLevel() * plugin.getCoreConfig().getXpPerLevel(); l().send(p, "island.level-info", "{level}", String.valueOf(is.getLevel()), "{xp}", String.valueOf(is.getExperience()), "{next}", String.valueOf(next)); }
    private void cmdAutosell(Player p) { plugin.getAutosellListener().toggleAutosell(p.getUniqueId()); l().send(p, plugin.getAutosellListener().isAutosellEnabled(p.getUniqueId()) ? "autosell.enabled" : "autosell.disabled"); }
    private void cmdChest(Player p) { if (noIsland(p)) return; Island is = getIsland(p); plugin.getIslandChestManager().open(p, is.getId(), is.getName()); }
    private void cmdBiome(Player p, String[] args) { if (noIsland(p)) return; if (args.length < 2) { new BiomeSelectMenu(p, plugin, getIsland(p)).open(); return; } org.bukkit.block.Biome b = plugin.getBiomeService().parseBiome(args[1]); if (b == null) { l().send(p, "biome.invalid", "{biome}", args[1]); return; } l().send(p, "biome.changing"); plugin.getBiomeService().changeBiome(getIsland(p), b); l().send(p, "biome.changed", "{biome}", b.name()); }
    private void cmdLogs(Player p) { if (noIsland(p)) return; java.util.List<me.reil.skybound.core.island.IslandLogEntry> logs = plugin.getIslandLogManager().getLogs(getIsland(p).getId(), 10); if (logs.isEmpty()) { l().send(p, "logs.empty"); return; } l().send(p, "logs.header"); for (me.reil.skybound.core.island.IslandLogEntry e : logs) { long ago = (System.currentTimeMillis() - e.getTimestamp()) / 1000L; String t = ago < 60 ? ago + "s" : (ago < 3600 ? (ago/60) + "m" : (ago/3600) + "h"); l().send(p, "logs.entry", "{time}", t, "{player}", e.getPlayerName(), "{action}", e.getAction().name().toLowerCase()); } }
    private void cmdDelete(Player p) { if (noIsland(p)) return; Island is = getIsland(p); if (!is.getOwner().equals(p.getUniqueId())) { l().send(p, "island.owner-only"); return; } boolean need = plugin.getConfirmationManager().requestConfirmation(p.getUniqueId(), "delete"); if (need) { l().send(p, "confirm.delete"); return; } plugin.getIslandManager().deleteIsland(is.getId()); plugin.getIslandChestManager().removeChest(is.getId()); l().send(p, "island.deleted"); }
    private void cmdRegen(Player p, String[] args) { if (noIsland(p)) return; Island is = getIsland(p); if (!is.getOwner().equals(p.getUniqueId())) { l().send(p, "island.owner-only"); return; } boolean need = plugin.getConfirmationManager().requestConfirmation(p.getUniqueId(), "regen"); if (need) { l().send(p, "confirm.regen"); return; } String s = args.length > 1 ? args[1] : "default"; plugin.getIslandManager().regenerateIsland(is.getId(), s); plugin.getSchematicService().paste(s + ".schem", is.getCenter()); p.teleport(is.getHome()); l().send(p, "island.regenerated"); }
    private void cmdPrestige(Player p) { if (noIsland(p)) return; Island is = getIsland(p); if (!is.getOwner().equals(p.getUniqueId())) { l().send(p, "island.owner-only"); return; } me.reil.skybound.core.island.PrestigeManager pm = plugin.getPrestigeManager(); if (!pm.canPrestige(is)) { int cur = pm.getPrestigeLevel(is.getId()); if (cur >= pm.getMaxPrestige()) l().send(p, "prestige.max", "{current}", String.valueOf(cur), "{max}", String.valueOf(pm.getMaxPrestige())); else l().send(p, "prestige.cannot-level", "{level}", String.valueOf(pm.getMinLevelToPrestige()), "{current}", String.valueOf(is.getLevel())); return; } boolean need = plugin.getConfirmationManager().requestConfirmation(p.getUniqueId(), "prestige"); if (need) { int next = pm.getPrestigeLevel(is.getId()) + 1; l().send(p, "prestige.info", "{level}", String.valueOf(next), "{percent}", String.format("%.0f", next * 10.0)); l().send(p, "confirm.prestige"); return; } pm.prestige(p, is); int nl = pm.getPrestigeLevel(is.getId()); l().send(p, "prestige.success", "{percent}", String.format("%.0f", (pm.getMultiplier(is.getId()) - 1.0) * 100)); Bukkit.broadcastMessage(l().get("prestige.broadcast", "{player}", p.getName(), "{level}", String.valueOf(nl))); }
    private void cmdHelp(Player p) { l().send(p, "help.header"); l().send(p, "help.create"); l().send(p, "help.home"); l().send(p, "help.sethome"); l().send(p, "help.invite"); l().send(p, "help.kick"); l().send(p, "help.leave"); l().send(p, "help.visit"); l().send(p, "help.lock"); l().send(p, "help.top"); l().send(p, "help.bank"); l().send(p, "help.shop"); l().send(p, "help.missions"); l().send(p, "help.upgrades"); l().send(p, "help.boosters"); l().send(p, "help.warp"); l().send(p, "help.value"); l().send(p, "help.level"); l().send(p, "help.biome"); l().send(p, "help.autosell"); l().send(p, "help.chest"); l().send(p, "help.prestige"); l().send(p, "help.settings"); }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("create","home","sethome","invite","accept","deny","kick","leave","promote","demote","transfer","trust","untrust","coop","lock","name","visit","like","trade","warp","setwarp","delwarp","warps","top","bank","shop","missions","upgrades","boosters","members","settings","delete","regen","value","level","menu","help","biome","autosell","chest","logs","prestige");
            List<String> r = new ArrayList<String>();
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) r.add(s);
            return r;
        }
        if (args.length == 2) {
            String s = args[0].toLowerCase();
            if ("invite".equals(s)||"kick".equals(s)||"promote".equals(s)||"demote".equals(s)||"transfer".equals(s)||"trust".equals(s)||"untrust".equals(s)||"coop".equals(s)||"visit".equals(s)) {
                List<String> n = new ArrayList<String>();
                for (Player pl : Bukkit.getOnlinePlayers()) if (pl.getName().toLowerCase().startsWith(args[1].toLowerCase())) n.add(pl.getName());
                return n;
            }
        }
        return Collections.emptyList();
    }
}
