package me.reil.skybound.api.visit;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;

/**
 * Island visit and rating system.
 */
public interface VisitProvider {

    /** Visit another player's island. */
    void visit(Player player, Island island);

    /** Like the island the player is currently on. */
    boolean like(Player player, Island island);

    /** Get total likes for an island. */
    int getLikes(Island island);

    /** Check if player has already liked this island today. */
    boolean hasLikedToday(Player player, Island island);
}
