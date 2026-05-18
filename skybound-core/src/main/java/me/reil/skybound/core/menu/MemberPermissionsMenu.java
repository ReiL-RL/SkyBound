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

        // Back (slot 49)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "Back");
            back.setItemMeta(backMeta);
        }
        inventory.setItem(49, back);
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

    private enum PermState {
        DEFAULT, GRANTED, DENIED
    }
}
