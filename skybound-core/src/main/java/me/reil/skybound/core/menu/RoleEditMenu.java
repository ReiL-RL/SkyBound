package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Edit permissions for a specific role.
 * Each permission shown as a colored wool block (green=on, red=off).
 * Click to toggle. Save button applies changes.
 */
public final class RoleEditMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final IslandRole role;
    private final IslandPermission[] allPerms = IslandPermission.values();
    private final Set<IslandPermission> editingPerms;

    public RoleEditMenu(Player player, SkyBoundPlugin plugin, Island island, IslandRole role) {
        super(player);
        this.plugin = plugin;
        this.island = island;
        this.role = role;
        this.editingPerms = EnumSet.copyOf(plugin.getIslandPermissionManager().getRoleDefaults(role));
    }

    /** Constructor that preserves current editing state (for refresh without losing changes). */
    public RoleEditMenu(Player player, SkyBoundPlugin plugin, Island island, IslandRole role, Set<IslandPermission> currentEditing) {
        super(player);
        this.plugin = plugin;
        this.island = island;
        this.role = role;
        this.editingPerms = EnumSet.copyOf(currentEditing);
    }

    @Override
    public String getTitle() {
        return lang().get("menu.roles.title") + " - " + role.name();
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Permissions start at slot 0, up to 45
        for (int i = 0; i < allPerms.length && i < 45; i++) {
            IslandPermission perm = allPerms[i];
            boolean enabled = editingPerms.contains(perm);

            ItemStack item = new ItemStack(enabled ? Material.GREEN_WOOL : Material.RED_WOOL);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((enabled ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ") + formatPerm(perm));
                List<String> lore = new ArrayList<String>();
                lore.add(enabled ? lang().get("menu.roles.enabled") : lang().get("menu.roles.disabled"));
                lore.add(lang().get("menu.roles.click-toggle"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        // Bottom row: Save and Back
        ItemStack save = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta saveMeta = save.getItemMeta();
        if (saveMeta != null) {
            saveMeta.setDisplayName(lang().get("menu.roles.save"));
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.roles.save-lore"));
            saveMeta.setLore(lore);
            save.setItemMeta(saveMeta);
        }
        inventory.setItem(49, save);

        ItemStack back = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(lang().get("menu.roles.cancel"));
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.roles.cancel-lore"));
            backMeta.setLore(lore);
            back.setItemMeta(backMeta);
        }
        inventory.setItem(45, back);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 45) {
            new RolePermissionsMenu(player, plugin, island).open();
            return;
        }

        if (slot == 49) {
            plugin.getIslandPermissionManager().setRolePermissions(island.getId(), role, editingPerms);
            lang().send(player, "menu.roles.saved", "{role}", role.name());
            new RolePermissionsMenu(player, plugin, island).open();
            return;
        }

        // Toggle permission
        if (slot >= 0 && slot < allPerms.length && slot < 45) {
            IslandPermission perm = allPerms[slot];
            if (editingPerms.contains(perm)) {
                editingPerms.remove(perm);
            } else {
                editingPerms.add(perm);
            }
            // Refresh with current state preserved
            new RoleEditMenu(player, plugin, island, role, editingPerms).open();
        }
    }

    private String formatPerm(IslandPermission perm) {
        return lang().get("permission." + perm.name().toLowerCase().replace('_', '-'));
    }
}
