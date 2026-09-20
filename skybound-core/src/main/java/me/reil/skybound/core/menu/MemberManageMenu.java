package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.SkyBoundPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Member management menu.
 * Shows: player info, role selector (click to set role), permissions button, kick, transfer.
 * Roles shown as wool blocks — current role is enchanted/highlighted.
 */
public final class MemberManageMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final UUID targetId;
    private final String targetName;

    private static final IslandRole[] ASSIGNABLE_ROLES = {
            IslandRole.MEMBER, IslandRole.TRUSTED, IslandRole.MODERATOR, IslandRole.ADMIN
    };

    public MemberManageMenu(Player player, SkyBoundPlugin plugin, Island island, UUID targetId) {
        super(player);
        this.plugin = plugin;
        this.island = island;
        this.targetId = targetId;
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        this.targetName = target.getName() != null ? target.getName() : targetId.toString().substring(0, 8);
    }

    @Override
    public String getTitle() {
        return lang().get("menu.manage.title", "{player}", targetName);
    }

    @Override
    public int getSize() {
        return 45;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        IslandRole currentRole = island.getMemberRole(targetId);
        IslandRole myRole = island.getMemberRole(player.getUniqueId());

        // Player head (slot 4)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetId));
            headMeta.setDisplayName(ChatColor.YELLOW + targetName);
            List<String> lore = new ArrayList<String>();
            lore.add(lang().get("menu.manage.current-role-line", "{role}", getRoleColor(currentRole) + currentRole.name()));
            lore.add("");
            lore.add(lang().get("menu.manage.info1"));
            lore.add(lang().get("menu.manage.info2"));
            headMeta.setLore(lore);
            head.setItemMeta(headMeta);
        }
        inventory.setItem(4, head);

        // === Role selector row (slots 10-13) ===
        ItemStack roleLabel = makeItem(Material.OAK_SIGN, lang().get("menu.manage.set-role"));
        inventory.setItem(9, roleLabel);

        int[] roleSlots = {10, 11, 12, 13};
        for (int i = 0; i < ASSIGNABLE_ROLES.length; i++) {
            IslandRole role = ASSIGNABLE_ROLES[i];
            boolean isCurrent = role == currentRole;
            Material mat = isCurrent ? Material.GOLD_BLOCK : getRoleWool(role);

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String prefix = isCurrent ? ChatColor.BOLD + "▶ " : "";
                meta.setDisplayName(getRoleColor(role) + prefix + role.name());
                List<String> lore = new ArrayList<String>();
                if (isCurrent) {
                    lore.add(lang().get("menu.manage.current"));
                } else {
                    lore.add(lang().get("menu.manage.click-role"));
                }
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(roleSlots[i], item);
        }

        // === Action buttons row (slots 28-34) ===

        // Permissions (slot 29)
        if (myRole == IslandRole.OWNER) {
            inventory.setItem(29, makeActionItem(Material.COMPARATOR, lang().get("menu.manage.permissions"),
                    lang().get("menu.manage.permissions-lore")));
        }

        // Kick (slot 31)
        inventory.setItem(31, makeActionItem(Material.BARRIER, lang().get("menu.manage.kick"),
                lang().get("menu.manage.kick-lore")));

        // Transfer (slot 33) — only owner sees this
        if (myRole == IslandRole.OWNER) {
            inventory.setItem(33, makeActionItem(Material.GOLDEN_HELMET, lang().get("menu.manage.transfer"),
                    lang().get("menu.manage.transfer-lore")));
        }

        addBackButton(40);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // Role assignment
        int[] roleSlots = {10, 11, 12, 13};
        for (int i = 0; i < roleSlots.length; i++) {
            if (slot == roleSlots[i]) {
                IslandRole newRole = ASSIGNABLE_ROLES[i];
                IslandRole currentRole = island.getMemberRole(targetId);
                if (newRole == currentRole) return;
                if (!canChangeRole(currentRole, newRole)) {
                    lang().send(player, newRole.isAtLeast(currentRole) ? "team.cannot-promote" : "team.cannot-demote");
                    return;
                }

                island.setMemberRole(targetId, newRole);
                if (newRole.isAtLeast(IslandRole.MEMBER)) {
                    plugin.getIslandManager().registerMember(targetId, island.getId());
                } else {
                    plugin.getIslandManager().unregisterMember(targetId, island.getId());
                }
                plugin.getIslandManager().saveData();
                lang().send(player, "team.role-changed", "{player}", targetName, "{role}", getRoleColor(newRole) + newRole.name());
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    lang().send(target, "team.role-changed-target", "{role}", getRoleColor(newRole) + newRole.name());
                }
                new MemberManageMenu(player, plugin, island, targetId).open();
                return;
            }
        }

        switch (slot) {
            case 29: // Permissions
                if (!island.getOwner().equals(player.getUniqueId())) {
                    lang().send(player, "no-permission");
                    return;
                }
                new MemberPermissionsMenu(player, plugin, island, targetId).open();
                break;

            case 31: // Kick
                boolean kicked = plugin.getTeamManager().kick(island, player.getUniqueId(), targetId);
                if (kicked) {
                    lang().send(player, "team.kicked", "{player}", targetName);
                    Player kickedPlayer = Bukkit.getPlayer(targetId);
                    if (kickedPlayer != null) lang().send(kickedPlayer, "team.kicked-target");
                    new IslandMembersMenu(player, plugin, island).open();
                } else {
                    lang().send(player, "team.cannot-kick");
                }
                break;

            case 33: // Transfer
                if (!island.getOwner().equals(player.getUniqueId())) break;
                boolean ok = plugin.getTeamManager().transferOwnership(island, player.getUniqueId(), targetId);
                if (ok) {
                    lang().send(player, "team.transferred", "{player}", targetName);
                    Player newOwner = Bukkit.getPlayer(targetId);
                    if (newOwner != null) lang().send(newOwner, "team.transferred-target");
                }
                player.closeInventory();
                break;

            case 40: // Back
                new IslandMembersMenu(player, plugin, island).open();
                break;
        }
    }

    private Material getRoleWool(IslandRole role) {
        switch (role) {
            case MEMBER: return Material.GREEN_WOOL;
            case TRUSTED: return Material.YELLOW_WOOL;
            case MODERATOR: return Material.CYAN_WOOL;
            case ADMIN: return Material.RED_WOOL;
            default: return Material.WHITE_WOOL;
        }
    }

    private String getRoleColor(IslandRole role) {
        switch (role) {
            case OWNER: return ChatColor.GOLD.toString();
            case ADMIN: return ChatColor.RED.toString();
            case MODERATOR: return ChatColor.AQUA.toString();
            case MEMBER: return ChatColor.GREEN.toString();
            case TRUSTED: return ChatColor.YELLOW.toString();
            case COOP: return ChatColor.GRAY.toString();
            default: return ChatColor.DARK_GRAY.toString();
        }
    }

    private boolean canChangeRole(IslandRole currentRole, IslandRole newRole) {
        if (targetId.equals(island.getOwner())) return false;

        IslandRole myRole = island.getMemberRole(player.getUniqueId());
        if (currentRole.isAtLeast(myRole) || newRole.isAtLeast(myRole)) {
            return false;
        }

        IslandPermission permission = newRole.isAtLeast(currentRole)
                ? IslandPermission.PROMOTE
                : IslandPermission.DEMOTE;
        return plugin.getIslandPermissionManager().hasPermission(island, player.getUniqueId(), permission);
    }

    private ItemStack makeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeActionItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<String>();
            for (String line : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
