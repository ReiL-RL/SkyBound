package me.reil.skybound.core.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Base class for all GUI menus.
 */
public abstract class Menu implements InventoryHolder {

    protected final Player player;
    protected Inventory inventory;

    public Menu(Player player) {
        this.player = player;
    }

    /** Create and populate the inventory. */
    public abstract void build();

    /** Handle a click in this menu. */
    public abstract void onClick(InventoryClickEvent event);

    /** Menu title. */
    public abstract String getTitle();

    /** Menu size (must be multiple of 9). */
    public abstract int getSize();

    /** Open the menu for the player. */
    public void open() {
        build();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected Inventory createInventory(String title, int size) {
        this.inventory = Bukkit.createInventory(this, size, title);
        return this.inventory;
    }

    /** Get LangManager from plugin instance. */
    protected me.reil.skybound.core.lang.LangManager lang() {
        return me.reil.skybound.core.SkyBoundPlugin.class.cast(
            Bukkit.getPluginManager().getPlugin("SkyBound")).getLangManager();
    }

    /** Translate color codes in a string. */
    protected String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
