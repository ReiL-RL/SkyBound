package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages island permissions with two layers:
 * 1. Role-based defaults (what each role can do by default)
 * 2. Per-member overrides (individual permissions that override role defaults)
 *
 * This allows owners to give specific members extra permissions or restrict them
 * beyond their role's defaults.
 */
public final class IslandPermissionManager {

    // Role -> default permissions for that role
    private static final Map<IslandRole, Set<IslandPermission>> ROLE_DEFAULTS = new EnumMap<IslandRole, Set<IslandPermission>>(IslandRole.class);

    // islandId -> memberId -> set of granted permissions (overrides)
    private final Map<String, Map<UUID, Set<IslandPermission>>> memberGrants = new LinkedHashMap<String, Map<UUID, Set<IslandPermission>>>();
    // islandId -> memberId -> set of denied permissions (overrides)
    private final Map<String, Map<UUID, Set<IslandPermission>>> memberDenials = new LinkedHashMap<String, Map<UUID, Set<IslandPermission>>>();

    // islandId -> role -> custom permissions (island-level role config)
    private final Map<String, Map<IslandRole, Set<IslandPermission>>> islandRoleOverrides = new LinkedHashMap<String, Map<IslandRole, Set<IslandPermission>>>();

    private final JavaPlugin plugin;
    private final File dataFile;

    static {
        // VISITOR: nothing
        ROLE_DEFAULTS.put(IslandRole.VISITOR, EnumSet.of(
                IslandPermission.TELEPORT_HOME,
                IslandPermission.TELEPORT_WARP
        ));

        // COOP: basic interaction
        ROLE_DEFAULTS.put(IslandRole.COOP, EnumSet.of(
                IslandPermission.TELEPORT_HOME,
                IslandPermission.TELEPORT_WARP,
                IslandPermission.OPEN_CHEST,
                IslandPermission.OPEN_FURNACE
        ));

        // TRUSTED: build + containers
        ROLE_DEFAULTS.put(IslandRole.TRUSTED, EnumSet.of(
                IslandPermission.BLOCK_PLACE,
                IslandPermission.BLOCK_BREAK,
                IslandPermission.BUCKET_USE,
                IslandPermission.OPEN_CHEST,
                IslandPermission.OPEN_BARREL,
                IslandPermission.OPEN_FURNACE,
                IslandPermission.OPEN_HOPPER,
                IslandPermission.OPEN_BREWING,
                IslandPermission.OPEN_ANVIL,
                IslandPermission.OPEN_ENCHANTING,
                IslandPermission.KILL_ANIMALS,
                IslandPermission.KILL_MONSTERS,
                IslandPermission.BREED_ANIMALS,
                IslandPermission.TELEPORT_HOME,
                IslandPermission.TELEPORT_WARP,
                IslandPermission.PORTAL_USE
        ));

        // MEMBER: trusted + some management
        EnumSet<IslandPermission> memberPerms = EnumSet.copyOf(ROLE_DEFAULTS.get(IslandRole.TRUSTED));
        memberPerms.add(IslandPermission.REDSTONE_INTERACT);
        memberPerms.add(IslandPermission.OPEN_SHULKER);
        memberPerms.add(IslandPermission.SHEAR);
        memberPerms.add(IslandPermission.LEASH);
        memberPerms.add(IslandPermission.RIDE);
        memberPerms.add(IslandPermission.BANK_DEPOSIT);
        memberPerms.add(IslandPermission.PLACE_GENERATOR);
        memberPerms.add(IslandPermission.FLY);
        ROLE_DEFAULTS.put(IslandRole.MEMBER, memberPerms);

        // MODERATOR: member + kick/invite
        EnumSet<IslandPermission> modPerms = EnumSet.copyOf(ROLE_DEFAULTS.get(IslandRole.MEMBER));
        modPerms.add(IslandPermission.INVITE);
        modPerms.add(IslandPermission.KICK);
        modPerms.add(IslandPermission.SET_WARP);
        modPerms.add(IslandPermission.DELETE_WARP);
        modPerms.add(IslandPermission.BANK_WITHDRAW);
        modPerms.add(IslandPermission.MANAGE_SHOP);
        ROLE_DEFAULTS.put(IslandRole.MODERATOR, modPerms);

        // ADMIN: moderator + settings
        EnumSet<IslandPermission> adminPerms = EnumSet.copyOf(ROLE_DEFAULTS.get(IslandRole.MODERATOR));
        adminPerms.add(IslandPermission.BAN);
        adminPerms.add(IslandPermission.PROMOTE);
        adminPerms.add(IslandPermission.DEMOTE);
        adminPerms.add(IslandPermission.SET_HOME);
        adminPerms.add(IslandPermission.CHANGE_NAME);
        adminPerms.add(IslandPermission.CHANGE_DESCRIPTION);
        adminPerms.add(IslandPermission.LOCK_ISLAND);
        adminPerms.add(IslandPermission.CHANGE_BIOME);
        adminPerms.add(IslandPermission.CHANGE_BORDER);
        adminPerms.add(IslandPermission.CHANGE_SETTINGS);
        adminPerms.add(IslandPermission.PURCHASE_UPGRADE);
        adminPerms.add(IslandPermission.PURCHASE_BOOSTER);
        adminPerms.add(IslandPermission.UPGRADE_GENERATOR);
        ROLE_DEFAULTS.put(IslandRole.ADMIN, adminPerms);

        // OWNER: everything
        ROLE_DEFAULTS.put(IslandRole.OWNER, EnumSet.allOf(IslandPermission.class));
    }

    public IslandPermissionManager() {
        this.plugin = null;
        this.dataFile = null;
    }

    public IslandPermissionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/island-permissions.yml");
        load();
    }

    /**
     * Check if a player has a specific permission on an island.
     * Checks: per-member override > island role override > global role default.
     */
    public boolean hasPermission(Island island, UUID playerId, IslandPermission permission) {
        // Check per-member denial first
        Map<UUID, Set<IslandPermission>> denials = memberDenials.get(island.getId());
        if (denials != null) {
            Set<IslandPermission> denied = denials.get(playerId);
            if (denied != null && denied.contains(permission)) {
                return false;
            }
        }

        // Check per-member grant
        Map<UUID, Set<IslandPermission>> grants = memberGrants.get(island.getId());
        if (grants != null) {
            Set<IslandPermission> granted = grants.get(playerId);
            if (granted != null && granted.contains(permission)) {
                return true;
            }
        }

        // Check island-level role override
        IslandRole role = island.getMemberRole(playerId);
        Map<IslandRole, Set<IslandPermission>> roleOverrides = islandRoleOverrides.get(island.getId());
        if (roleOverrides != null) {
            Set<IslandPermission> rolePerms = roleOverrides.get(role);
            if (rolePerms != null) {
                return rolePerms.contains(permission);
            }
        }

        // Fall back to global role defaults
        Set<IslandPermission> defaults = ROLE_DEFAULTS.get(role);
        return defaults != null && defaults.contains(permission);
    }

    /**
     * Grant a specific permission to a member (override).
     */
    public void grantPermission(String islandId, UUID playerId, IslandPermission permission) {
        Map<UUID, Set<IslandPermission>> grants = memberGrants.get(islandId);
        if (grants == null) {
            grants = new LinkedHashMap<UUID, Set<IslandPermission>>();
            memberGrants.put(islandId, grants);
        }
        Set<IslandPermission> perms = grants.get(playerId);
        if (perms == null) {
            perms = EnumSet.noneOf(IslandPermission.class);
            grants.put(playerId, perms);
        }
        perms.add(permission);

        // Remove from denials if present
        Map<UUID, Set<IslandPermission>> denials = memberDenials.get(islandId);
        if (denials != null) {
            Set<IslandPermission> denied = denials.get(playerId);
            if (denied != null) denied.remove(permission);
        }
        cleanupMemberOverrides(islandId, playerId);
        save();
    }

    /**
     * Deny a specific permission from a member (override).
     */
    public void denyPermission(String islandId, UUID playerId, IslandPermission permission) {
        Map<UUID, Set<IslandPermission>> denials = memberDenials.get(islandId);
        if (denials == null) {
            denials = new LinkedHashMap<UUID, Set<IslandPermission>>();
            memberDenials.put(islandId, denials);
        }
        Set<IslandPermission> perms = denials.get(playerId);
        if (perms == null) {
            perms = EnumSet.noneOf(IslandPermission.class);
            denials.put(playerId, perms);
        }
        perms.add(permission);

        // Remove from grants if present
        Map<UUID, Set<IslandPermission>> grants = memberGrants.get(islandId);
        if (grants != null) {
            Set<IslandPermission> granted = grants.get(playerId);
            if (granted != null) granted.remove(permission);
        }
        cleanupMemberOverrides(islandId, playerId);
        save();
    }

    /**
     * Reset a member's permission override (use role default).
     */
    public void resetPermission(String islandId, UUID playerId, IslandPermission permission) {
        Map<UUID, Set<IslandPermission>> grants = memberGrants.get(islandId);
        if (grants != null) {
            Set<IslandPermission> perms = grants.get(playerId);
            if (perms != null) perms.remove(permission);
        }
        Map<UUID, Set<IslandPermission>> denials = memberDenials.get(islandId);
        if (denials != null) {
            Set<IslandPermission> perms = denials.get(playerId);
            if (perms != null) perms.remove(permission);
        }
        cleanupMemberOverrides(islandId, playerId);
        save();
    }

    /**
     * Set a custom permission set for a role on a specific island.
     */
    public void setRolePermissions(String islandId, IslandRole role, Set<IslandPermission> permissions) {
        Map<IslandRole, Set<IslandPermission>> overrides = islandRoleOverrides.get(islandId);
        if (overrides == null) {
            overrides = new EnumMap<IslandRole, Set<IslandPermission>>(IslandRole.class);
            islandRoleOverrides.put(islandId, overrides);
        }
        overrides.put(role, copyPermissions(permissions));
        save();
    }

    /**
     * Get the default permissions for a role.
     */
    public Set<IslandPermission> getRoleDefaults(IslandRole role) {
        Set<IslandPermission> defaults = ROLE_DEFAULTS.get(role);
        return defaults != null ? Collections.unmodifiableSet(defaults) : Collections.<IslandPermission>emptySet();
    }

    public Set<IslandPermission> getRolePermissions(String islandId, IslandRole role) {
        Map<IslandRole, Set<IslandPermission>> overrides = islandRoleOverrides.get(islandId);
        Set<IslandPermission> custom = overrides == null ? null : overrides.get(role);
        if (custom != null) {
            return Collections.unmodifiableSet(custom);
        }
        return getRoleDefaults(role);
    }

    public void removeIsland(String islandId) {
        if (islandId == null || islandId.isEmpty()) return;
        boolean changed = memberGrants.remove(islandId) != null;
        changed = memberDenials.remove(islandId) != null || changed;
        changed = islandRoleOverrides.remove(islandId) != null || changed;
        if (changed) {
            save();
        }
    }

    public void removeMember(String islandId, UUID playerId) {
        if (islandId == null || islandId.isEmpty() || playerId == null) return;
        boolean changed = false;
        Map<UUID, Set<IslandPermission>> grants = memberGrants.get(islandId);
        if (grants != null) {
            changed = grants.remove(playerId) != null;
            if (grants.isEmpty()) memberGrants.remove(islandId);
        }
        Map<UUID, Set<IslandPermission>> denials = memberDenials.get(islandId);
        if (denials != null) {
            changed = denials.remove(playerId) != null || changed;
            if (denials.isEmpty()) memberDenials.remove(islandId);
        }
        if (changed) {
            save();
        }
    }

    /**
     * Get per-member grants for an island (for persistence).
     */
    public Map<String, Map<UUID, Set<IslandPermission>>> getAllGrants() {
        return memberGrants;
    }

    /**
     * Get per-member denials for an island (for persistence).
     */
    public Map<String, Map<UUID, Set<IslandPermission>>> getAllDenials() {
        return memberDenials;
    }

    private void cleanupMemberOverrides(String islandId, UUID playerId) {
        cleanupMemberMap(memberGrants, islandId, playerId);
        cleanupMemberMap(memberDenials, islandId, playerId);
    }

    private void cleanupMemberMap(Map<String, Map<UUID, Set<IslandPermission>>> source, String islandId, UUID playerId) {
        Map<UUID, Set<IslandPermission>> islandMap = source.get(islandId);
        if (islandMap == null) return;
        Set<IslandPermission> permissions = islandMap.get(playerId);
        if (permissions != null && permissions.isEmpty()) {
            islandMap.remove(playerId);
        }
        if (islandMap.isEmpty()) {
            source.remove(islandId);
        }
    }

    private void load() {
        memberGrants.clear();
        memberDenials.clear();
        islandRoleOverrides.clear();
        if (dataFile == null || !dataFile.exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
        loadMemberOverrides(cfg.getConfigurationSection("member-grants"), memberGrants);
        loadMemberOverrides(cfg.getConfigurationSection("member-denials"), memberDenials);

        ConfigurationSection rolesRoot = cfg.getConfigurationSection("role-overrides");
        if (rolesRoot != null) {
            for (String islandId : rolesRoot.getKeys(false)) {
                ConfigurationSection islandSection = rolesRoot.getConfigurationSection(islandId);
                if (islandSection == null) continue;
                Map<IslandRole, Set<IslandPermission>> roleMap = new EnumMap<IslandRole, Set<IslandPermission>>(IslandRole.class);
                for (String roleName : islandSection.getKeys(false)) {
                    IslandRole role = parseRole(roleName);
                    if (role == null) continue;
                    Set<IslandPermission> permissions = parsePermissions(islandSection.getStringList(roleName));
                    roleMap.put(role, permissions);
                }
                if (!roleMap.isEmpty()) {
                    islandRoleOverrides.put(islandId, roleMap);
                }
            }
        }
    }

    private void loadMemberOverrides(ConfigurationSection root, Map<String, Map<UUID, Set<IslandPermission>>> target) {
        if (root == null) return;
        for (String islandId : root.getKeys(false)) {
            ConfigurationSection islandSection = root.getConfigurationSection(islandId);
            if (islandSection == null) continue;
            Map<UUID, Set<IslandPermission>> memberMap = new LinkedHashMap<UUID, Set<IslandPermission>>();
            for (String uuidString : islandSection.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    Set<IslandPermission> permissions = parsePermissions(islandSection.getStringList(uuidString));
                    if (!permissions.isEmpty()) {
                        memberMap.put(playerId, permissions);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!memberMap.isEmpty()) {
                target.put(islandId, memberMap);
            }
        }
    }

    private void save() {
        if (plugin == null || dataFile == null) return;
        YamlConfiguration cfg = new YamlConfiguration();
        saveMemberOverrides(cfg, "member-grants", memberGrants);
        saveMemberOverrides(cfg, "member-denials", memberDenials);

        for (Map.Entry<String, Map<IslandRole, Set<IslandPermission>>> islandEntry : islandRoleOverrides.entrySet()) {
            for (Map.Entry<IslandRole, Set<IslandPermission>> roleEntry : islandEntry.getValue().entrySet()) {
                cfg.set("role-overrides." + islandEntry.getKey() + "." + roleEntry.getKey().name(),
                        serializePermissions(roleEntry.getValue()));
            }
        }

        me.reil.skybound.core.storage.YamlFiles.saveAtomically(plugin, cfg, dataFile, "island-permissions.yml");
    }

    private void saveMemberOverrides(YamlConfiguration cfg, String root, Map<String, Map<UUID, Set<IslandPermission>>> source) {
        for (Map.Entry<String, Map<UUID, Set<IslandPermission>>> islandEntry : source.entrySet()) {
            for (Map.Entry<UUID, Set<IslandPermission>> playerEntry : islandEntry.getValue().entrySet()) {
                if (!playerEntry.getValue().isEmpty()) {
                    cfg.set(root + "." + islandEntry.getKey() + "." + playerEntry.getKey().toString(),
                            serializePermissions(playerEntry.getValue()));
                }
            }
        }
    }

    private List<String> serializePermissions(Set<IslandPermission> permissions) {
        List<String> out = new ArrayList<String>();
        for (IslandPermission permission : permissions) {
            out.add(permission.name());
        }
        return out;
    }

    private Set<IslandPermission> parsePermissions(List<String> raw) {
        Set<IslandPermission> out = EnumSet.noneOf(IslandPermission.class);
        for (String value : raw) {
            try {
                out.add(IslandPermission.valueOf(value.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    private IslandRole parseRole(String value) {
        try {
            return IslandRole.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Set<IslandPermission> copyPermissions(Set<IslandPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return EnumSet.noneOf(IslandPermission.class);
        }
        return EnumSet.copyOf(permissions);
    }
}
