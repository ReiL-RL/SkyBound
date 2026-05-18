package me.reil.skybound.api.island;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Core island management interface.
 * Provides access to island data, creation, deletion, and queries.
 */
public interface IslandProvider {

    /**
     * Get island by its unique id.
     */
    Island getIsland(String islandId);

    /**
     * Get the island a player belongs to (as owner or member).
     */
    Island getPlayerIsland(UUID playerId);

    /**
     * Get all loaded islands.
     */
    Collection<Island> getAllIslands();

    /**
     * Create a new island for a player with the given schematic.
     * @return the created island, or null if creation failed
     */
    Island createIsland(Player owner, String schematicName);

    /**
     * Delete an island permanently.
     */
    boolean deleteIsland(String islandId);

    /**
     * Regenerate an island with a new schematic (reset).
     */
    boolean regenerateIsland(String islandId, String schematicName);

    /**
     * Check if a location is within any island's boundaries.
     */
    Island getIslandAt(Location location);

    /**
     * Get the island's home/spawn location.
     */
    Location getIslandHome(String islandId);

    /**
     * Set the island's home/spawn location.
     */
    void setIslandHome(String islandId, Location location);

    /**
     * Get the total number of islands.
     */
    int getIslandCount();
}
