package me.reil.skybound.api.team;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Team management provider.
 * Handles invites, kicks, promotions, co-op, and trust.
 */
public interface TeamProvider {

    /**
     * Invite a player to an island.
     * @return true if invite was sent
     */
    boolean invite(Island island, UUID inviter, UUID target);

    /**
     * Accept a pending invite.
     */
    boolean acceptInvite(UUID playerId, String islandId);

    /**
     * Deny a pending invite.
     */
    boolean denyInvite(UUID playerId, String islandId);

    /**
     * Kick a member from an island.
     */
    boolean kick(Island island, UUID kicker, UUID target);

    /**
     * Ban a player from an island.
     */
    boolean ban(Island island, UUID banner, UUID target);

    /**
     * Unban a player from an island.
     */
    boolean unban(Island island, UUID unbanner, UUID target);

    /**
     * Promote a member.
     */
    boolean promote(Island island, UUID promoter, UUID target);

    /**
     * Demote a member.
     */
    boolean demote(Island island, UUID demoter, UUID target);

    /**
     * Transfer island ownership.
     */
    boolean transferOwnership(Island island, UUID currentOwner, UUID newOwner);

    /**
     * Add a player as co-op (temporary access without joining).
     */
    boolean addCoop(Island island, UUID adder, UUID target);

    /**
     * Remove co-op access.
     */
    boolean removeCoop(Island island, UUID remover, UUID target);

    /**
     * Trust a player (persistent access without joining).
     */
    boolean trust(Island island, UUID truster, UUID target);

    /**
     * Untrust a player.
     */
    boolean untrust(Island island, UUID untruster, UUID target);

    /**
     * Check if a player has a pending invite to an island.
     */
    boolean hasPendingInvite(UUID playerId, String islandId);

    /**
     * Get the maximum team size for an island (considering upgrades).
     */
    int getMaxTeamSize(Island island);
}
