package me.reil.skybound.core.listener;

import me.reil.skybound.core.economy.VaultEconomyProvider;
import me.reil.skybound.core.shop.ShopManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Autosell: when a player's inventory is full and they pick up items,
 * automatically sell them if they have autosell enabled.
 */
public final class AutosellListener implements Listener {

    private final ShopManager shopManager;
    private final VaultEconomyProvider economy;
    private final Map<UUID, Boolean> autosellEnabled = new LinkedHashMap<UUID, Boolean>();

    public AutosellListener(ShopManager shopManager, VaultEconomyProvider economy) {
        this.shopManager = shopManager;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (!isAutosellEnabled(player.getUniqueId())) return;

        // Check if inventory is full
        if (player.getInventory().firstEmpty() != -1) return;

        // Try to sell the item
        ItemStack item = event.getItem().getItemStack();
        int amount = item.getAmount();

        double sellPrice = shopManager.getSellPrice(item);
        if (sellPrice <= 0) return;

        double total = sellPrice * amount;
        economy.deposit(player.getUniqueId(), total);
        event.getItem().remove();
        event.setCancelled(true);
    }

    public void toggleAutosell(UUID playerId) {
        Boolean current = autosellEnabled.get(playerId);
        autosellEnabled.put(playerId, current == null || !current);
    }

    public boolean isAutosellEnabled(UUID playerId) {
        Boolean enabled = autosellEnabled.get(playerId);
        return enabled != null && enabled;
    }

}
