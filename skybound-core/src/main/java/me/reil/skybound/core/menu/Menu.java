package me.reil.skybound.core.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all GUI menus.
 */
public abstract class Menu implements InventoryHolder {

    protected final Player player;
    protected Inventory inventory;
    /** Optional reference to the menu we came from — clicking "Back" reopens it. */
    protected Menu parentMenu;

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

    /** Set the parent menu used by the Back button. Returns this for chaining. */
    public Menu withParent(Menu parent) {
        this.parentMenu = parent;
        return this;
    }

    public Menu getParentMenu() {
        return parentMenu;
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
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Place a "Back" button in the last slot of the inventory (or specified slot).
     * Subclasses should call this from build() when they want a back navigation,
     * and call {@link #handleBack(int)} from onClick() to wire it.
     */
    protected void addBackButton() {
        addBackButton(getSize() - 1);
    }

    protected void addBackButton(int slot) {
        if (inventory == null) return;
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(lang().get("menu.back"));
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.back-lore"));
            meta.setLore(lore);
            back.setItemMeta(meta);
        }
        inventory.setItem(slot, back);
    }

    /**
     * If the click is on the back-button slot, navigate back and return true.
     * Returns false otherwise so the subclass can continue handling.
     */
    protected boolean handleBack(int slot) {
        return handleBack(slot, getSize() - 1);
    }

    protected boolean handleBack(int slot, int backSlot) {
        if (slot != backSlot) return false;
        if (parentMenu != null) {
            parentMenu.open();
        } else {
            // Default fallback: open island main menu
            try {
                me.reil.skybound.core.SkyBoundPlugin plugin =
                        (me.reil.skybound.core.SkyBoundPlugin) Bukkit.getPluginManager().getPlugin("SkyBound");
                if (plugin != null) {
                    me.reil.skybound.api.island.Island island =
                            plugin.getIslandManager().getPlayerIsland(player.getUniqueId());
                    if (island != null) {
                        new IslandMainMenu(player, plugin, island).open();
                    } else {
                        player.closeInventory();
                    }
                } else {
                    player.closeInventory();
                }
            } catch (Throwable t) {
                player.closeInventory();
            }
        }
        return true;
    }
}
