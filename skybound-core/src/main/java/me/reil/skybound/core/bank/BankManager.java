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
        if (player == null || island == null || !isValidAmount(amount)) return false;
        if (!economy.has(player.getUniqueId(), amount)) return false;

        double limit = getBankLimit(island);
        if (island.getBankBalance() + amount > limit) return false;

        if (!economy.withdraw(player.getUniqueId(), amount)) return false;
        island.setBankBalance(island.getBankBalance() + amount);
        recordTransaction(island.getId(), player.getUniqueId(), BankTransaction.TransactionType.DEPOSIT, amount);
        logToIsland(island, player, me.reil.skybound.core.island.IslandLogEntry.LogAction.BANK_DEPOSIT,
                String.format("%.0f", amount));
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean withdraw(Player player, Island island, double amount) {
        if (player == null || island == null || !isValidAmount(amount)) return false;
        if (island.getBankBalance() < amount) return false;

        if (!economy.deposit(player.getUniqueId(), amount)) return false;
        island.setBankBalance(island.getBankBalance() - amount);
        recordTransaction(island.getId(), player.getUniqueId(), BankTransaction.TransactionType.WITHDRAW, amount);
        logToIsland(island, player, me.reil.skybound.core.island.IslandLogEntry.LogAction.BANK_WITHDRAW,
                String.format("%.0f", amount));
        islandManager.saveData();
        return true;
    }

    private void logToIsland(Island island, Player player,
                             me.reil.skybound.core.island.IslandLogEntry.LogAction action, String details) {
        try {
            if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin) {
                ((me.reil.skybound.core.SkyBoundPlugin) plugin).getIslandLogManager().log(
                        island.getId(), player.getUniqueId(), player.getName(), action, details);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public double getBankLimit(Island island) {
        double bonus = 0.0;
        try {
            if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin && island != null) {
                bonus = ((me.reil.skybound.core.SkyBoundPlugin) plugin).getUpgradeManager().getEffectiveValue(island, "bank_capacity");
            }
        } catch (RuntimeException ignored) {
            bonus = 0.0;
        }
        if (Double.isNaN(bonus) || Double.isInfinite(bonus) || bonus < 0.0) {
            bonus = 0.0;
        }
        return config.getBaseBankLimit() + bonus;
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
        if (island == null || !isValidAmount(amount)) return false;
        if (island.getBankBalance() < amount) return false;
        island.setBankBalance(island.getBankBalance() - amount);
        islandManager.saveData();
        return true;
    }

    public boolean depositInternal(Island island, double amount) {
        if (island == null || !isValidAmount(amount)) return false;
        double limit = getBankLimit(island);
        if (island.getBankBalance() + amount > limit) return false;
        island.setBankBalance(island.getBankBalance() + amount);
        islandManager.saveData();
        return true;
    }

    private boolean isValidAmount(double amount) {
        return amount > 0.0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
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
