package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.SkyBoundPlugin;
import me.reil.skybound.core.island.IslandPermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-member permission editor.
 * Shows all permissions as wool blocks:
 *   GREEN = granted (override)
 *   RED = denied (override)
 *   GRAY = default (from role)
 * Click cycles: Default -> Grant -> Deny -> Default
 */
public final class MemberPermissionsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final UUID targetId;
    private final String targetName;
    private final IslandPermission[] allPerms = IslandPermission.values();

    public MemberPermissionsMenu(Player player, SkyBoundPlugin plugin, Island island, UUID targetId) {
        super(player);
        this.plugin = plugin;
        this.island = island;
        this.targetId = targetId;
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        this.targetName = target.getName() != null ? target.getName() : targetId.toString().substring(0, 8);
    }

    @Override
    public String getTitle() {
        return lang().get("menu.perms.title", "{player}", targetName);
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        IslandPermissionManager pm = plugin.getIslandPermissionManager();

        // Permissions (slots 0-44)
        for (int i = 0; i < allPerms.length && i < 45; i++) {
            IslandPermission perm = allPerms[i];
            PermState state = getState(island.getId(), targetId, perm);

            Material mat;
            String stateText;
            switch (state) {
                case GRANTED:
                    mat = Material.GREEN_WOOL;
                    stateText = lang().get("menu.perms.granted");
                    break;
                case DENIED:
                    mat = Material.RED_WOOL;
                    stateText = lang().get("menu.perms.denied");
                    break;
                default:
                    mat = Material.GRAY_WOOL;
                    boolean fromRole = pm.hasPermission(island, targetId, perm);
                    stateText = lang().get(fromRole ? "menu.perms.default-allowed" : "menu.perms.default-denied");
                    break;
            }

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.WHITE + formatPerm(perm));
                List<String> lore = new ArrayList<String>();
                lore.add(stateText);
                lore.add("");
                lore.add(lang().get("menu.perms.click-cycle"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(i, item);
        }

        addBackButton(49);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 49) {
            new MemberManageMenu(player, plugin, island, targetId).open();
            return;
        }

        if (slot < 0 || slot >= allPerms.length || slot >= 45) return;

        IslandPermission perm = allPerms[slot];
        IslandPermissionManager pm = plugin.getIslandPermissionManager();
        PermState state = getState(island.getId(), targetId, perm);

        // Cycle: Default -> Grant -> Deny -> Default
        switch (state) {
            case DEFAULT:
                pm.grantPermission(island.getId(), targetId, perm);
                break;
            case GRANTED:
                pm.denyPermission(island.getId(), targetId, perm);
                break;
            case DENIED:
                pm.resetPermission(island.getId(), targetId, perm);
                break;
        }

        // Refresh
        new MemberPermissionsMenu(player, plugin, island, targetId).open();
    }

    private PermState getState(String islandId, UUID playerId, IslandPermission permission) {
        Map<String, Map<UUID, Set<IslandPermission>>> grants = plugin.getIslandPermissionManager().getAllGrants();
        Map<String, Map<UUID, Set<IslandPermission>>> denials = plugin.getIslandPermissionManager().getAllDenials();

        Map<UUID, Set<IslandPermission>> islandGrants = grants.get(islandId);
        if (islandGrants != null) {
            Set<IslandPermission> playerGrants = islandGrants.get(playerId);
            if (playerGrants != null && playerGrants.contains(permission)) {
                return PermState.GRANTED;
            }
        }

        Map<UUID, Set<IslandPermission>> islandDenials = denials.get(islandId);
        if (islandDenials != null) {
            Set<IslandPermission> playerDenials = islandDenials.get(playerId);
            if (playerDenials != null && playerDenials.contains(permission)) {
                return PermState.DENIED;
            }
        }

        return PermState.DEFAULT;
    }

    private String formatPerm(IslandPermission perm) {
        return lang().get("permission." + perm.name().toLowerCase().replace('_', '-'));
    }

    private enum PermState {
        DEFAULT, GRANTED, DENIED
    }
}
