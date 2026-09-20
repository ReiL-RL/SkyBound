package me.reil.skybound.api.island;

import org.bukkit.entity.Player;

/**
 * Island prestige system provider.
 * Manages prestige levels per island, and allows third-party plugins
 * to read/modify prestige and trigger prestige actions.
 */
public interface PrestigeProvider {

    /**
     * Get the prestige level of an island.
     */
    int getPrestigeLevel(String islandId);

    /**
     * Set the prestige level directly. Use with caution — bypasses checks.
     */
    void setPrestigeLevel(String islandId, int level);

    /**
     * Get the multiplier for an island (1.0 = base, 1.1 = +10%, etc).
     */
    double getMultiplier(String islandId);

    /**
     * Whether the island meets requirements to prestige (level, max cap).
     */
    boolean canPrestige(Island island);

    /**
     * Perform a full prestige: reset island progress, increment prestige.
     * @return true if successful
     */
    boolean prestige(Player player, Island island);

    /**
     * Minimum island level required to prestige.
     */
    int getMinLevelToPrestige();

    /**
     * Maximum prestige level.
     */
    int getMaxPrestige();
}
