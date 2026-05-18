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
                meta.setDisplayName((enabled ? ChatColor.GREEN + "\u2714 " : ChatColor.RED + "\u2718 ") + formatPerm(perm));
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
            player.sendMessage(lang().get("menu.roles.saved", "{role}", role.name()));
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
        switch (perm) {
            case BLOCK_PLACE: return "\u0421\u0442\u0430\u0432\u0438\u0442\u044c \u0431\u043b\u043e\u043a\u0438";
            case BLOCK_BREAK: return "\u041b\u043e\u043c\u0430\u0442\u044c \u0431\u043b\u043e\u043a\u0438";
            case BUCKET_USE: return "\u0412\u0451\u0434\u0440\u0430";
            case REDSTONE_INTERACT: return "\u0420\u0435\u0434\u0441\u0442\u043e\u0443\u043d";
            case OPEN_CHEST: return "\u0421\u0443\u043d\u0434\u0443\u043a\u0438";
            case OPEN_BARREL: return "\u0411\u043e\u0447\u043a\u0438";
            case OPEN_SHULKER: return "\u0428\u0430\u043b\u043a\u0435\u0440\u044b";
            case OPEN_FURNACE: return "\u041f\u0435\u0447\u0438";
            case OPEN_HOPPER: return "\u0412\u043e\u0440\u043e\u043d\u043a\u0438";
            case OPEN_BREWING: return "\u0417\u0435\u043b\u044c\u0435\u0432\u0430\u0440\u043a\u0430";
            case OPEN_ANVIL: return "\u041d\u0430\u043a\u043e\u0432\u0430\u043b\u044c\u043d\u044f";
            case OPEN_ENCHANTING: return "\u0417\u0430\u0447\u0430\u0440\u043e\u0432\u0430\u043d\u0438\u0435";
            case KILL_ANIMALS: return "\u0423\u0431\u0438\u0432\u0430\u0442\u044c \u0436\u0438\u0432\u043e\u0442\u043d\u044b\u0445";
            case KILL_MONSTERS: return "\u0423\u0431\u0438\u0432\u0430\u0442\u044c \u043c\u043e\u0431\u043e\u0432";
            case BREED_ANIMALS: return "\u0420\u0430\u0437\u0432\u0435\u0434\u0435\u043d\u0438\u0435";
            case SHEAR: return "\u0421\u0442\u0440\u0438\u0436\u043a\u0430";
            case LEASH: return "\u041f\u043e\u0432\u043e\u0434\u043e\u043a";
            case RIDE: return "\u0415\u0437\u0434\u0430";
            case INVITE: return "\u041f\u0440\u0438\u0433\u043b\u0430\u0448\u0430\u0442\u044c";
            case KICK: return "\u0418\u0441\u043a\u043b\u044e\u0447\u0430\u0442\u044c";
            case BAN: return "\u0411\u0430\u043d\u0438\u0442\u044c";
            case PROMOTE: return "\u041f\u043e\u0432\u044b\u0448\u0430\u0442\u044c";
            case DEMOTE: return "\u041f\u043e\u043d\u0438\u0436\u0430\u0442\u044c";
            case SET_HOME: return "\u0423\u0441\u0442. \u0434\u043e\u043c";
            case SET_WARP: return "\u0423\u0441\u0442. \u0432\u0430\u0440\u043f";
            case DELETE_WARP: return "\u0423\u0434. \u0432\u0430\u0440\u043f";
            case CHANGE_NAME: return "\u041f\u0435\u0440\u0435\u0438\u043c\u0435\u043d\u043e\u0432\u0430\u0442\u044c";
            case CHANGE_DESCRIPTION: return "\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435";
            case LOCK_ISLAND: return "\u0417\u0430\u043a\u0440\u044b\u0442\u044c \u043e\u0441\u0442\u0440\u043e\u0432";
            case CHANGE_BIOME: return "\u0421\u043c\u0435\u043d\u0430 \u0431\u0438\u043e\u043c\u0430";
            case CHANGE_BORDER: return "\u0413\u0440\u0430\u043d\u0438\u0446\u0430";
            case CHANGE_SETTINGS: return "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438";
            case BANK_DEPOSIT: return "\u0412\u043d\u0435\u0441\u0442\u0438 \u0432 \u0431\u0430\u043d\u043a";
            case BANK_WITHDRAW: return "\u0412\u044b\u0432\u0435\u0441\u0442\u0438 \u0438\u0437 \u0431\u0430\u043d\u043a\u0430";
            case PURCHASE_UPGRADE: return "\u041a\u0443\u043f\u0438\u0442\u044c \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u0435";
            case PURCHASE_BOOSTER: return "\u041a\u0443\u043f\u0438\u0442\u044c \u0431\u0443\u0441\u0442\u0435\u0440";
            case PLACE_GENERATOR: return "\u0421\u0442\u0430\u0432\u0438\u0442\u044c \u0433\u0435\u043d\u0435\u0440\u0430\u0442\u043e\u0440";
            case BREAK_GENERATOR: return "\u041b\u043e\u043c\u0430\u0442\u044c \u0433\u0435\u043d\u0435\u0440\u0430\u0442\u043e\u0440";
            case UPGRADE_GENERATOR: return "\u0423\u043b\u0443\u0447\u0448\u0438\u0442\u044c \u0433\u0435\u043d.";
            case PORTAL_USE: return "\u041f\u043e\u0440\u0442\u0430\u043b\u044b";
            case FLY: return "\u041f\u043e\u043b\u0451\u0442";
            case TELEPORT_HOME: return "\u0422\u0435\u043b\u0435\u043f\u043e\u0440\u0442 \u0434\u043e\u043c\u043e\u0439";
            case TELEPORT_WARP: return "\u0422\u0435\u043b\u0435\u043f\u043e\u0440\u0442 \u0432\u0430\u0440\u043f";
            case ISLAND_REGEN: return "\u041f\u0435\u0440\u0435\u0441\u043e\u0437\u0434\u0430\u043d\u0438\u0435";
            default: return perm.name();
        }
    }
}
