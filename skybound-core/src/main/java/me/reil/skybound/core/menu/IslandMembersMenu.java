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
 * Island members menu.
 * Shows all members with their roles.
 * Click on a member to open their management menu (promote, demote, kick, permissions, transfer).
 */
public final class IslandMembersMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;
    private final List<UUID> memberList = new ArrayList<UUID>();

    public IslandMembersMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override
    public String getTitle() {
        return lang().get("menu.members.title");
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        // Fill border
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);

        // Members
        int slot = 9;
        for (UUID memberId : island.getMembers()) {
            if (slot >= 45) break;
            memberList.add(memberId);

            OfflinePlayer member = Bukkit.getOfflinePlayer(memberId);
            IslandRole role = island.getMemberRole(memberId);
            String name = member.getName() != null ? member.getName() : memberId.toString().substring(0, 8);
            boolean isOwner = memberId.equals(island.getOwner());
            boolean isOnline = member.isOnline();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(member);
                String color = isOwner ? "\u00a76" : (role == IslandRole.ADMIN ? "\u00a7c" : (role == IslandRole.MODERATOR ? "\u00a7b" : "\u00a7a"));
                meta.setDisplayName(color + name);
                List<String> lore = new ArrayList<String>();
                lore.add(color("&7") + lang().get("menu.members.role") + getRoleColor(role) + role.name());
                lore.add(color("&7") + "Status: " + (isOnline ? color("&a") + lang().get("menu.members.online") : color("&c") + lang().get("menu.members.offline")));
                lore.add("");
                if (!isOwner) {
                    lore.add(lang().get("menu.members.manage"));
                } else {
                    lore.add(lang().get("menu.members.owner"));
                }
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inventory.setItem(slot++, head);
        }

        // Back button
        inventory.setItem(49, makeItem(Material.ARROW, lang().get("button.back")));

        // Info
        ItemStack info = makeItem(Material.OAK_SIGN, lang().get("menu.members.count", "{count}", String.valueOf(island.getMembers().size()), "{max}", String.valueOf(plugin.getUpgradeManager().getTeamSizeLimit(island))));
        inventory.setItem(4, info);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 49) {
            new IslandMainMenu(player, plugin, island).open();
            return;
        }

        int index = slot - 9;
        if (index < 0 || index >= memberList.size()) return;

        UUID targetId = memberList.get(index);
        if (targetId.equals(island.getOwner())) return; // Can't manage owner

        // Open member management menu
        new MemberManageMenu(player, plugin, island, targetId).open();
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
}
