package me.reil.skybound.core.economy;

import me.reil.skybound.api.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Economy provider backed by Vault.
 */
public final class VaultEconomyProvider implements EconomyProvider {

    private final JavaPlugin plugin;
    private Economy vault;

    public VaultEconomyProvider(JavaPlugin plugin) {
        this.plugin = plugin;
        setupVault();
    }

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features will be limited.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            vault = rsp.getProvider();
            plugin.getLogger().info("Vault economy hooked: " + vault.getName());
        } else {
            plugin.getLogger().warning("No Vault economy provider found!");
        }
    }

    @Override
    public double getBalance(UUID playerId) {
        if (vault == null) return 0.0;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return vault.getBalance(player);
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        if (vault == null || amount <= 0) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return vault.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        if (vault == null || amount <= 0) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return vault.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        if (vault == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return vault.has(player, amount);
    }

    @Override
    public String format(double amount) {
        if (vault == null) return String.format("$%.2f", amount);
        return vault.format(amount);
    }

    @Override
    public String getCurrencyName() {
        if (vault == null) return "coin";
        return vault.currencyNameSingular();
    }

    @Override
    public String getCurrencyNamePlural() {
        if (vault == null) return "coins";
        return vault.currencyNamePlural();
    }

    public boolean isAvailable() {
        return vault != null;
    }
}
