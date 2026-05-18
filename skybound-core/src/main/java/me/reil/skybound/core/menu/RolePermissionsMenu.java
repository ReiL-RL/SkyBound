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
import java.util.List;
import java.util.Set;

/**
 * Role selection menu — pick a role to edit its permissions.
 */
public final class RolePermissionsMenu extends Menu {

    private final SkyBoundPlugin plugin;
    private final Island island;

    private static final IslandRole[] EDITABLE_ROLES = {
            IslandRole.VISITOR, IslandRole.COOP, IslandRole.TRUSTED, IslandRole.MEMBER, IslandRole.MODERATOR, IslandRole.ADMIN
    };

    public RolePermissionsMenu(Player player, SkyBoundPlugin plugin, Island island) {
        super(player);
        this.plugin = plugin;
        this.island = island;
    }

    @Override
    public String getTitle() {
        return lang().get("menu.roles.title");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public void build() {
        createInventory(getTitle(), getSize());

        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 18; i < 27; i++) inventory.setItem(i, border);

        int[] slots = {9, 10, 11, 12, 13, 14};
        for (int i = 0; i < EDITABLE_ROLES.length; i++) {
            IslandRole role = EDITABLE_ROLES[i];
            Set<IslandPermission> perms = plugin.getIslandPermissionManager().getRoleDefaults(role);

            Material mat = getRoleMaterial(role);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(getRoleColor(role) + role.name());
                List<String> lore = new ArrayList<String>();
                lore.add(lang().get("menu.roles.perms-count", "{count}", String.valueOf(perms.size())));
                lore.add("");
                lore.add(lang().get("menu.roles.edit"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slots[i], item);
        }

        inventory.setItem(4, makeInfoItem());
        inventory.setItem(22, makeItem(Material.ARROW, ChatColor.RED + "Back"));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 22) {
            new IslandSettingsMenu(player, plugin, island).open();
            return;
        }

        int[] slots = {9, 10, 11, 12, 13, 14};
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) {
                new RoleEditMenu(player, plugin, island, EDITABLE_ROLES[i]).open();
                return;
            }
        }
    }

    private Material getRoleMaterial(IslandRole role) {
        switch (role) {
            case VISITOR: return Material.GRAY_WOOL;
            case COOP: return Material.LIGHT_GRAY_WOOL;
            case TRUSTED: return Material.YELLOW_WOOL;
            case MEMBER: return Material.GREEN_WOOL;
            case MODERATOR: return Material.CYAN_WOOL;
            case ADMIN: return Material.RED_WOOL;
            default: return Material.GOLD_BLOCK;
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

    private ItemStack makeInfoItem() {
        ItemStack info = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Role Permissions");
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Select a role to edit");
            lore.add(ChatColor.GRAY + "its permissions.");
            meta.setLore(lore);
            info.setItemMeta(meta);
        }
        return info;
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
