package me.reil.skybound.core.listener;

import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.PlayerShopManager;
import me.reil.skybound.core.util.InventoryUtil;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * On right-click of a registered shop chest, opens a "buy" GUI showing chest contents
 * with the shop's price per stack. Click an item to buy it.
 */
public final class PlayerShopListener implements Listener {

    private final SkyBoundPlugin plugin;

    public PlayerShopListener(SkyBoundPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Chest)) return;

        PlayerShopManager.Shop shop = plugin.getPlayerShopManager().getShopAt(block.getLocation());
        if (shop == null) return;

        Player p = event.getPlayer();
        if (p.getUniqueId().equals(shop.owner)) return; // owner uses chest normally

        event.setCancelled(true);
        if (!isValidPrice(shop.price)) {
            plugin.getLangManager().send(p, "playershop.invalid-price");
            return;
        }

        // Buy first item with money
        Chest chest = (Chest) block.getState();
        Inventory inv = chest.getBlockInventory();
        ItemStack toBuy = null;
        int slot = -1;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && !it.getType().isAir()) {
                toBuy = it;
                slot = i;
                break;
            }
        }
        if (toBuy == null) {
            plugin.getLangManager().send(p, "playershop.empty");
            return;
        }

        if (!plugin.getEconomyProvider().has(p.getUniqueId(), shop.price)) {
            plugin.getLangManager().send(p, "playershop.no-money", "{price}", String.valueOf(shop.price));
            return;
        }

        ItemStack bought = toBuy.clone();
        bought.setAmount(1);
        if (!InventoryUtil.canFit(p.getInventory(), bought)) {
            plugin.getLangManager().send(p, "playershop.no-space");
            return;
        }
        if (!plugin.getEconomyProvider().withdraw(p.getUniqueId(), shop.price)) {
            plugin.getLangManager().send(p, "playershop.payment-failed");
            return;
        }
        if (!plugin.getEconomyProvider().deposit(shop.owner, shop.price)) {
            plugin.getEconomyProvider().deposit(p.getUniqueId(), shop.price);
            plugin.getLangManager().send(p, "playershop.payment-failed");
            return;
        }

        p.getInventory().addItem(bought);

        // Decrement chest
        if (toBuy.getAmount() > 1) {
            toBuy.setAmount(toBuy.getAmount() - 1);
            inv.setItem(slot, toBuy);
        } else {
            inv.setItem(slot, null);
        }

        plugin.getLangManager().send(p, "playershop.bought", "{price}", String.valueOf(shop.price));
        Player owner = org.bukkit.Bukkit.getPlayer(shop.owner);
        if (owner != null) {
            plugin.getLangManager().send(owner, "playershop.sold",
                    "{player}", p.getName(),
                    "{item}", bought.getType().name(),
                    "{price}", String.valueOf(shop.price));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Chest)) return;
        PlayerShopManager.Shop shop = plugin.getPlayerShopManager().getShopAt(block.getLocation());
        if (shop == null) return;
        if (!event.getPlayer().getUniqueId().equals(shop.owner)) {
            event.setCancelled(true);
            plugin.getLangManager().send(event.getPlayer(), "playershop.not-owner");
            return;
        }
        plugin.getPlayerShopManager().removeShop(block.getLocation());
        plugin.getLangManager().send(event.getPlayer(), "playershop.removed");
    }

    private boolean isValidPrice(double price) {
        return price > 0.0 && !Double.isNaN(price) && !Double.isInfinite(price);
    }

}
