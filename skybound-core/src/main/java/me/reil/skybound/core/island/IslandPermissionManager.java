package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
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
        overrides.put(role, EnumSet.copyOf(permissions));
    }

    /**
     * Get the default permissions for a role.
     */
    public Set<IslandPermission> getRoleDefaults(IslandRole role) {
        Set<IslandPermission> defaults = ROLE_DEFAULTS.get(role);
        return defaults != null ? Collections.unmodifiableSet(defaults) : Collections.<IslandPermission>emptySet();
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
}
