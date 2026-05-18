package me.reil.skybound.core.menu;

import me.reil.skybound.api.island.Island;
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
            lore.add(ChatColor.GRAY + "Current role: " + getRoleColor(currentRole) + currentRole.name());
            lore.add("");
            lore.add(ChatColor.GRAY + "Select a new role below,");
            lore.add(ChatColor.GRAY + "or use the action buttons.");
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
                String prefix = isCurrent ? ChatColor.BOLD + "\u25B6 " : "";
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
        inventory.setItem(29, makeActionItem(Material.COMPARATOR, lang().get("menu.manage.permissions"),
                lang().get("menu.manage.permissions-lore")));

        // Kick (slot 31)
        inventory.setItem(31, makeActionItem(Material.BARRIER, lang().get("menu.manage.kick"),
                lang().get("menu.manage.kick-lore")));

        // Transfer (slot 33) — only owner sees this
        if (myRole == IslandRole.OWNER) {
            inventory.setItem(33, makeActionItem(Material.GOLDEN_HELMET, lang().get("menu.manage.transfer"),
                    lang().get("menu.manage.transfer-lore")));
        }

        // Back (slot 40)
        inventory.setItem(40, makeItem(Material.ARROW, lang().get("button.back")));
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

                // Set role directly
                island.setMemberRole(targetId, newRole);
                player.sendMessage(ChatColor.GREEN + targetName + " is now " + getRoleColor(newRole) + newRole.name());
                Player target = Bukkit.getPlayer(targetId);
                if (target != null) {
                    target.sendMessage(ChatColor.GREEN + "Your role was changed to " + getRoleColor(newRole) + newRole.name());
                }
                new MemberManageMenu(player, plugin, island, targetId).open();
                return;
            }
        }

        switch (slot) {
            case 29: // Permissions
                new MemberPermissionsMenu(player, plugin, island, targetId).open();
                break;

            case 31: // Kick
                boolean kicked = plugin.getTeamManager().kick(island, player.getUniqueId(), targetId);
                if (kicked) {
                    plugin.getIslandManager().unregisterMember(targetId);
                    player.sendMessage(ChatColor.GREEN + targetName + " kicked.");
                    Player kickedPlayer = Bukkit.getPlayer(targetId);
                    if (kickedPlayer != null) kickedPlayer.sendMessage(ChatColor.RED + "You were kicked from the island.");
                    new IslandMembersMenu(player, plugin, island).open();
                } else {
                    player.sendMessage(ChatColor.RED + "Cannot kick this player.");
                }
                break;

            case 33: // Transfer
                if (!island.getOwner().equals(player.getUniqueId())) break;
                boolean ok = plugin.getTeamManager().transferOwnership(island, player.getUniqueId(), targetId);
                if (ok) {
                    player.sendMessage(ChatColor.GOLD + "Ownership transferred to " + targetName + "!");
                    Player newOwner = Bukkit.getPlayer(targetId);
                    if (newOwner != null) newOwner.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You are now the island owner!");
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
            case OWNER: return "\u00a76";
            case ADMIN: return "\u00a7c";
            case MODERATOR: return "\u00a7b";
            case MEMBER: return "\u00a7a";
            case TRUSTED: return "\u00a7e";
            case COOP: return "\u00a77";
            default: return "\u00a78";
        }
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
