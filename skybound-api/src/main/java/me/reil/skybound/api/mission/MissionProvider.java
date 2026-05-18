package me.reil.skybound.api.mission;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * Mission system provider.
 * Missions give players objectives with rewards and island XP.
 */
public interface MissionProvider {

    /**
     * Get all available mission definitions.
     */
    Collection<Mission> getMissions();

    /**
     * Get a specific mission by id.
     */
    Mission getMission(String missionId);

    /**
     * Get player's progress on a mission.
     */
    MissionProgress getProgress(UUID playerId, String missionId);

    /**
     * Add progress to a mission for a player/island.
     */
    void addProgress(UUID playerId, String missionId, int amount);

    /**
     * Check if a mission is completed by a player.
     */
    boolean isCompleted(UUID playerId, String missionId);

    /**
     * Claim rewards for a completed mission.
     * @return true if rewards were given
     */
    boolean claimReward(Player player, String missionId);

    /**
     * Get missions available for a specific island level.
     */
    Collection<Mission> getMissionsForLevel(int islandLevel);

    /**
     * Reset a player's mission progress (for repeatable missions).
     */
    void resetProgress(UUID playerId, String missionId);
}
