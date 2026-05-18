package me.reil.skybound.api.upgrade;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * Island upgrade system provider.
 * Upgrades are permanent improvements purchased with island bank or player money.
 */
public interface UpgradeProvider {

    /**
     * Get all available upgrade types.
     */
    Collection<Upgrade> getUpgrades();

    /**
     * Get a specific upgrade by id.
     */
    Upgrade getUpgrade(String upgradeId);

    /**
     * Get the current level of an upgrade for an island.
     */
    int getLevel(Island island, String upgradeId);

    /**
     * Purchase the next level of an upgrade.
     * @return true if purchase was successful
     */
    boolean purchase(Player buyer, Island island, String upgradeId);

    /**
     * Get the cost for the next level.
     */
    double getNextLevelCost(Island island, String upgradeId);

    /**
     * Check if an upgrade is at max level.
     */
    boolean isMaxLevel(Island island, String upgradeId);

    /**
     * Get the effective value of an upgrade at its current level.
     * For example, generator speed multiplier, island size bonus, etc.
     */
    double getEffectiveValue(Island island, String upgradeId);
}
