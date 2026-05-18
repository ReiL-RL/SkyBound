package me.reil.skybound.core.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Global listener for all SkyBound GUI menus.
 */
public final class MenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() == null) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Menu)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() != event.getInventory()) return;

        Menu menu = (Menu) holder;
        menu.onClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory() == null) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
