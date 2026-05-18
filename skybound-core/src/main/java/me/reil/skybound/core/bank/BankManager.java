package me.reil.skybound.core.bank;

import me.reil.skybound.api.bank.BankProvider;
import me.reil.skybound.api.bank.BankTransaction;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.economy.VaultEconomyProvider;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Island bank implementation.
 * Supports money deposits/withdrawals.
 */
public final class BankManager implements BankProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    private final VaultEconomyProvider economy;
    private final Map<String, List<BankTransactionImpl>> transactions = new LinkedHashMap<String, List<BankTransactionImpl>>();

    public BankManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager, VaultEconomyProvider economy) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.economy = economy;
    }

    @Override
    public double getBalance(Island island) {
        return island.getBankBalance();
    }

    @Override
    public boolean deposit(Player player, Island island, double amount) {
        if (amount <= 0 || !economy.has(player.getUniqueId(), amount)) return false;

        double limit = getBankLimit(island);
        if (island.getBankBalance() + amount > limit) return false;

        economy.withdraw(player.getUniqueId(), amount);
        island.setBankBalance(island.getBankBalance() + amount);
        recordTransaction(island.getId(), player.getUniqueId(), BankTransaction.TransactionType.DEPOSIT, amount);
        return true;
    }

    @Override
    public boolean withdraw(Player player, Island island, double amount) {
        if (amount <= 0 || island.getBankBalance() < amount) return false;

        island.setBankBalance(island.getBankBalance() - amount);
        economy.deposit(player.getUniqueId(), amount);
        recordTransaction(island.getId(), player.getUniqueId(), BankTransaction.TransactionType.WITHDRAW, amount);
        return true;
    }

    @Override
    public double getBankLimit(Island island) {
        // Base limit + upgrade bonus
        // TODO: Factor in bank upgrade level
        return 1000000.0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BankTransaction> getTransactions(Island island, int limit) {
        List<BankTransactionImpl> list = transactions.get(island.getId());
        if (list == null) return Collections.emptyList();
        int start = Math.max(0, list.size() - limit);
        return Collections.unmodifiableList((List<? extends BankTransaction>) (List<?>) list.subList(start, list.size()));
    }

    /**
     * Withdraw from bank internally (for upgrades/boosters).
     */
    public boolean withdrawInternal(Island island, double amount) {
        if (island.getBankBalance() < amount) return false;
        island.setBankBalance(island.getBankBalance() - amount);
        return true;
    }

    private void recordTransaction(String islandId, java.util.UUID playerId, BankTransaction.TransactionType type, double amount) {
        List<BankTransactionImpl> list = transactions.get(islandId);
        if (list == null) {
            list = new ArrayList<BankTransactionImpl>();
            transactions.put(islandId, list);
        }
        list.add(new BankTransactionImpl(playerId, type, amount, System.currentTimeMillis(), ""));
        // Keep last 100 transactions
        if (list.size() > 100) {
            list.remove(0);
        }
    }
}
