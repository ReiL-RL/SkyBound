package me.reil.skybound.core.integration;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI integration manager.
 * Registers the expansion when PAPI is available.
 *
 * Supported placeholders:
 *   %skybound_island_name%
 *   %skybound_island_level%
 *   %skybound_island_value%
 *   %skybound_island_members%
 *   %skybound_island_bank%
 *   %skybound_island_owner%
 *   %skybound_island_rank_value%
 *   %skybound_island_rank_level%
 *   %skybound_island_locked%
 *   %skybound_island_radius%
 *   %skybound_island_xp%
 *   %skybound_island_prestige%
 *   %skybound_island_multiplier%
 *   %skybound_has_island%
 *   %skybound_island_role%
 */
public final class PlaceholderExpansion {

    private final SkyBoundPlugin plugin;
    private boolean registered;

    public PlaceholderExpansion(SkyBoundPlugin plugin) {
        this.plugin = plugin;
        this.registered = false;
    }

    public void register() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            new SkyBoundPapiExpansion(plugin).register();
            registered = true;
            plugin.getLogger().info("PlaceholderAPI expansion registered (15 placeholders).");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PAPI expansion: " + e.getMessage());
        }
    }

    public boolean isRegistered() {
        return registered;
    }

    /**
     * Resolve a placeholder. Used by the PAPI expansion and internally.
     */
    public static String resolve(SkyBoundPlugin plugin, OfflinePlayer offlinePlayer, String identifier) {
        if (offlinePlayer == null) return "";

        Island island = plugin.getIslandManager().getPlayerIsland(offlinePlayer.getUniqueId());

        switch (identifier) {
            case "island_name":
                return island != null ? island.getName() : "Нет";
            case "island_level":
                return island != null ? String.valueOf(island.getLevel()) : "0";
            case "island_value":
                return island != null ? String.format("%.0f", island.getValue()) : "0";
            case "island_members":
                return island != null ? String.valueOf(island.getMembers().size()) : "0";
            case "island_bank":
                return island != null ? String.format("%.0f", island.getBankBalance()) : "0";
            case "island_owner":
                if (island == null) return "Нет";
                OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
                return owner.getName() != null ? owner.getName() : "Unknown";
            case "island_rank_value":
                if (island == null) return "-";
                int rv = plugin.getLeaderboardManager().getRankByValue(island.getId());
                return rv > 0 ? String.valueOf(rv) : "-";
            case "island_rank_level":
                if (island == null) return "-";
                int rl = plugin.getLeaderboardManager().getRankByLevel(island.getId());
                return rl > 0 ? String.valueOf(rl) : "-";
            case "island_locked":
                return island != null ? (island.isLocked() ? "Да" : "Нет") : "Нет";
            case "island_radius":
                return island != null ? String.valueOf(island.getRadius()) : "0";
            case "island_xp":
                return island != null ? String.valueOf(island.getExperience()) : "0";
            case "island_prestige":
                return island != null ? String.valueOf(plugin.getPrestigeManager().getPrestigeLevel(island.getId())) : "0";
            case "island_multiplier":
                if (island == null) return "0%";
                double mult = plugin.getPrestigeManager().getMultiplier(island.getId());
                return String.format("+%.0f%%", (mult - 1.0) * 100);
            case "has_island":
                return island != null ? "true" : "false";
            case "island_role":
                if (island == null) return "Нет";
                return island.getMemberRole(offlinePlayer.getUniqueId()).name();
            default:
                return null;
        }
    }
}
