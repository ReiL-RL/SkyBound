package me.reil.skybound.api.economy;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Economy provider interface.
 * Wraps Vault or provides a standalone island-currency system.
 */
public interface EconomyProvider {

    /**
     * Get player's balance.
     */
    double getBalance(UUID playerId);

    /**
     * Deposit money to a player.
     * @return true if successful
     */
    boolean deposit(UUID playerId, double amount);

    /**
     * Withdraw money from a player.
     * @return true if successful
     */
    boolean withdraw(UUID playerId, double amount);

    /**
     * Check if player has enough money.
     */
    boolean has(UUID playerId, double amount);

    /**
     * Format a money amount to display string.
     */
    String format(double amount);

    /**
     * Get the currency name (singular).
     */
    String getCurrencyName();

    /**
     * Get the currency name (plural).
     */
    String getCurrencyNamePlural();
}
