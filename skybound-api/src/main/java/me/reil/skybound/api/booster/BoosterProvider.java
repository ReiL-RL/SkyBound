package me.reil.skybound.api.booster;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Booster system provider.
 * Boosters are temporary island-wide buffs that can be purchased.
 * Inspired by IridiumSkyblock's booster system.
 */
public interface BoosterProvider {

    /**
     * Get all available booster definitions.
     */
    Collection<Booster> getBoosters();

    /**
     * Get a specific booster by id.
     */
    Booster getBooster(String boosterId);

    /**
     * Purchase and activate a booster for an island.
     * @return true if purchase was successful
     */
    boolean purchase(Player buyer, Island island, String boosterId);

    /**
     * Check if a booster is currently active on an island.
     */
    boolean isActive(Island island, String boosterId);

    /**
     * Get remaining seconds for an active booster.
     */
    long getRemainingSeconds(Island island, String boosterId);

    /**
     * Get the multiplier value of an active booster.
     * Returns 1.0 if not active.
     */
    double getMultiplier(Island island, String boosterId);

    /**
     * Get all active boosters for an island.
     */
    Collection<ActiveBooster> getActiveBoosters(Island island);
}
