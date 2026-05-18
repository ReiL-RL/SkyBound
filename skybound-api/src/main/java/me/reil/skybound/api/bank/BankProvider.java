package me.reil.skybound.api.bank;

import me.reil.skybound.api.island.Island;
import org.bukkit.entity.Player;

/**
 * Island bank provider.
 * Each island has a shared bank that members can deposit/withdraw from.
 */
public interface BankProvider {

    /**
     * Get the island's money balance.
     */
    double getBalance(Island island);

    /**
     * Deposit money into the island bank.
     * @return true if successful
     */
    boolean deposit(Player player, Island island, double amount);

    /**
     * Withdraw money from the island bank.
     * @return true if successful
     */
    boolean withdraw(Player player, Island island, double amount);

    /**
     * Get the bank limit for an island (considering upgrades).
     */
    double getBankLimit(Island island);

    /**
     * Get transaction log for an island.
     */
    java.util.List<BankTransaction> getTransactions(Island island, int limit);
}
