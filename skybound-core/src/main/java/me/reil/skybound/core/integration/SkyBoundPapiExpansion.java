package me.reil.skybound.core.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI expansion for SkyBound.
 * Prefix: skybound
 *
 * All placeholders:
 *   %skybound_island_name%        - Island name
 *   %skybound_island_level%       - Island level
 *   %skybound_island_value%       - Island value (block worth)
 *   %skybound_island_members%     - Member count
 *   %skybound_island_bank%        - Bank balance
 *   %skybound_island_owner%       - Owner name
 *   %skybound_island_rank_value%  - Rank by value
 *   %skybound_island_rank_level%  - Rank by level
 *   %skybound_island_locked%      - Locked status
 *   %skybound_island_radius%      - Island radius
 *   %skybound_island_xp%          - Island XP
 *   %skybound_island_prestige%    - Prestige level
 *   %skybound_island_multiplier%  - Prestige multiplier
 *   %skybound_has_island%         - Has island (true/false)
 *   %skybound_island_role%        - Player's role on island
 */
public final class SkyBoundPapiExpansion extends PlaceholderExpansion {

    private final SkyBoundPlugin plugin;

    public SkyBoundPapiExpansion(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "skybound";
    }

    @Override
    public String getAuthor() {
        return "Reil";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        String result = me.reil.skybound.core.integration.PlaceholderExpansion.resolve(plugin, player, identifier);
        return result != null ? result : "";
    }
}
