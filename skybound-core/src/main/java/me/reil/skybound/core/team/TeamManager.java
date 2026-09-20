package me.reil.skybound.core.team;

import me.reil.skybound.api.event.IslandMemberJoinEvent;
import me.reil.skybound.api.event.IslandMemberLeaveEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.api.team.TeamProvider;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Team management implementation.
 * Handles invites, kicks, promotions, co-op, and trust.
 */
public final class TeamManager implements TeamProvider {

    private static final String BANNED_PLAYERS_SETTING = "team.banned-players";

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    // Map: invited player -> island id
    private final Map<UUID, String> pendingInvites = new HashMap<UUID, String>();

    public TeamManager(JavaPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @Override
    public boolean invite(Island island, UUID inviter, UUID target) {
        if (island == null || island.getMembers().contains(target)) return false;
        if (isBanned(island, target)) return false;
        if (islandManager.getPlayerIsland(target) != null) return false;
        if (countPermanentMembers(island) >= getMaxTeamSize(island)) return false;
        pendingInvites.put(target, island.getId());
        return true;
    }

    @Override
    public boolean acceptInvite(UUID playerId, String islandId) {
        String pending = pendingInvites.get(playerId);
        if (pending == null || !pending.equals(islandId)) return false;

        Island island = islandManager.getIsland(islandId);
        if (island == null) return false;
        if (isBanned(island, playerId)) {
            pendingInvites.remove(playerId);
            return false;
        }
        if (islandManager.getPlayerIsland(playerId) != null) {
            pendingInvites.remove(playerId);
            return false;
        }
        if (countPermanentMembers(island) >= getMaxTeamSize(island)) {
            pendingInvites.remove(playerId);
            return false;
        }

        IslandMemberJoinEvent event = new IslandMemberJoinEvent(island, playerId, IslandRole.MEMBER);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        island.addMember(playerId, IslandRole.MEMBER);
        islandManager.registerMember(playerId, islandId);
        pendingInvites.remove(playerId);
        logIsland(island, playerId, me.reil.skybound.core.island.IslandLogEntry.LogAction.MEMBER_JOIN, "");
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean denyInvite(UUID playerId, String islandId) {
        String pending = pendingInvites.get(playerId);
        if (pending == null || !pending.equals(islandId)) return false;
        pendingInvites.remove(playerId);
        return true;
    }

    @Override
    public boolean kick(Island island, UUID kicker, UUID target) {
        if (island == null || target.equals(island.getOwner())) return false;
        if (!island.getMembers().contains(target)) return false;

        IslandRole kickerRole = island.getMemberRole(kicker);
        IslandRole targetRole = island.getMemberRole(target);
        if (!hasPermission(island, kicker, IslandPermission.KICK) || targetRole.isAtLeast(kickerRole)) return false;

        IslandMemberLeaveEvent event = new IslandMemberLeaveEvent(island, target, IslandMemberLeaveEvent.LeaveReason.KICKED);
        Bukkit.getPluginManager().callEvent(event);

        island.removeMember(target);
        islandManager.unregisterMember(target, island.getId());
        removeMemberPermissions(island, target);
        logIsland(island, target, me.reil.skybound.core.island.IslandLogEntry.LogAction.MEMBER_KICK, "");
        islandManager.saveData();
        return true;
    }

    /** Helper: log to island journal. */
    private void logIsland(Island island, UUID playerId,
                           me.reil.skybound.core.island.IslandLogEntry.LogAction action, String details) {
        try {
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);
            String name = op.getName() != null ? op.getName() : playerId.toString().substring(0, 8);
            if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin) {
                ((me.reil.skybound.core.SkyBoundPlugin) plugin).getIslandLogManager().log(
                        island.getId(), playerId, name, action, details);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean ban(Island island, UUID banner, UUID target) {
        if (island == null || target.equals(island.getOwner())) return false;
        IslandRole bannerRole = island.getMemberRole(banner);
        IslandRole targetRole = island.getMemberRole(target);
        if (!hasPermission(island, banner, IslandPermission.BAN) || targetRole.isAtLeast(bannerRole)) return false;

        List<String> bannedPlayers = getBannedPlayers(island);
        String targetId = target.toString();
        if (!bannedPlayers.contains(targetId)) {
            bannedPlayers.add(targetId);
            island.setSetting(BANNED_PLAYERS_SETTING, bannedPlayers);
        }

        pendingInvites.remove(target);
        if (island.getMembers().contains(target)) {
            island.removeMember(target);
            islandManager.unregisterMember(target, island.getId());
            removeMemberPermissions(island, target);
            logIsland(island, target, me.reil.skybound.core.island.IslandLogEntry.LogAction.MEMBER_KICK, "banned");
        }
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean unban(Island island, UUID unbanner, UUID target) {
        if (island == null) return false;
        if (!hasPermission(island, unbanner, IslandPermission.BAN)) return false;

        List<String> bannedPlayers = getBannedPlayers(island);
        boolean removed = bannedPlayers.remove(target.toString());
        if (removed) {
            island.setSetting(BANNED_PLAYERS_SETTING, bannedPlayers);
            islandManager.saveData();
        }
        return removed;
    }

    public boolean isBanned(Island island, UUID playerId) {
        return island != null && playerId != null && getBannedPlayers(island).contains(playerId.toString());
    }

    @Override
    public boolean promote(Island island, UUID promoter, UUID target) {
        if (island == null || !island.getMembers().contains(target)) return false;
        IslandRole current = island.getMemberRole(target);
        IslandRole promoterRole = island.getMemberRole(promoter);

        if (current == IslandRole.OWNER || !hasPermission(island, promoter, IslandPermission.PROMOTE)) return false;

        IslandRole next = getNextRole(current);
        if (next == null || next.isAtLeast(promoterRole)) return false;

        island.setMemberRole(target, next);
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean demote(Island island, UUID demoter, UUID target) {
        if (island == null || !island.getMembers().contains(target)) return false;
        IslandRole current = island.getMemberRole(target);
        IslandRole demoterRole = island.getMemberRole(demoter);

        if (current == IslandRole.MEMBER || !hasPermission(island, demoter, IslandPermission.DEMOTE)) return false;
        if (current.isAtLeast(demoterRole)) return false;

        IslandRole prev = getPreviousRole(current);
        if (prev == null) return false;

        island.setMemberRole(target, prev);
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean transferOwnership(Island island, UUID currentOwner, UUID newOwner) {
        if (island == null || !island.getOwner().equals(currentOwner)) return false;
        if (!island.getMembers().contains(newOwner)) return false;
        if (!island.getMemberRole(newOwner).isAtLeast(IslandRole.MEMBER)) return false;

        island.setMemberRole(currentOwner, IslandRole.ADMIN);
        island.setMemberRole(newOwner, IslandRole.OWNER);
        island.setOwner(newOwner);
        islandManager.registerMember(currentOwner, island.getId());
        islandManager.registerMember(newOwner, island.getId());
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean addCoop(Island island, UUID adder, UUID target) {
        if (island == null || island.getMembers().contains(target)) return false;
        if (!hasPermission(island, adder, IslandPermission.INVITE)) return false;
        if (isBanned(island, target)) return false;
        island.addMember(target, IslandRole.COOP);
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean removeCoop(Island island, UUID remover, UUID target) {
        if (island == null) return false;
        if (!hasPermission(island, remover, IslandPermission.KICK)) return false;
        if (island.getMemberRole(target) != IslandRole.COOP) return false;
        island.removeMember(target);
        removeMemberPermissions(island, target);
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean trust(Island island, UUID truster, UUID target) {
        if (island == null || island.getMembers().contains(target)) return false;
        if (!hasPermission(island, truster, IslandPermission.INVITE)) return false;
        if (isBanned(island, target)) return false;
        island.addMember(target, IslandRole.TRUSTED);
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean untrust(Island island, UUID untruster, UUID target) {
        if (island == null) return false;
        if (!hasPermission(island, untruster, IslandPermission.KICK)) return false;
        if (island.getMemberRole(target) != IslandRole.TRUSTED) return false;
        island.removeMember(target);
        removeMemberPermissions(island, target);
        islandManager.saveData();
        return true;
    }

    @Override
    public boolean hasPendingInvite(UUID playerId, String islandId) {
        String pending = pendingInvites.get(playerId);
        return pending != null && pending.equals(islandId);
    }

    @Override
    public int getMaxTeamSize(Island island) {
        if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin) {
            me.reil.skybound.core.SkyBoundPlugin skyBound = (me.reil.skybound.core.SkyBoundPlugin) plugin;
            me.reil.skybound.core.upgrade.UpgradeManager upgradeManager = skyBound.getUpgradeManager();
            if (upgradeManager != null) {
                return upgradeManager.getTeamSizeLimit(island);
            }
            return skyBound.getCoreConfig().getMaxTeamSize();
        }
        return 4;
    }

    private int countPermanentMembers(Island island) {
        int count = 0;
        for (UUID member : island.getMembers()) {
            IslandRole role = island.getMemberRole(member);
            if (role.isAtLeast(IslandRole.MEMBER)) {
                count++;
            }
        }
        return count;
    }

    private List<String> getBannedPlayers(Island island) {
        Object raw = island.getSettings().get(BANNED_PLAYERS_SETTING);
        List<String> bannedPlayers = new ArrayList<String>();
        if (raw instanceof Iterable<?>) {
            for (Object value : (Iterable<?>) raw) {
                if (value != null) {
                    bannedPlayers.add(String.valueOf(value));
                }
            }
        } else if (raw instanceof String && !((String) raw).isEmpty()) {
            bannedPlayers.add((String) raw);
        }
        return bannedPlayers;
    }

    private boolean hasPermission(Island island, UUID playerId, IslandPermission permission) {
        if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin) {
            return ((me.reil.skybound.core.SkyBoundPlugin) plugin)
                    .getIslandPermissionManager()
                    .hasPermission(island, playerId, permission);
        }
        return island.getMemberRole(playerId).isAtLeast(IslandRole.OWNER);
    }

    private void removeMemberPermissions(Island island, UUID playerId) {
        if (plugin instanceof me.reil.skybound.core.SkyBoundPlugin) {
            ((me.reil.skybound.core.SkyBoundPlugin) plugin)
                    .getIslandPermissionManager()
                    .removeMember(island.getId(), playerId);
        }
    }

    private IslandRole getNextRole(IslandRole current) {
        switch (current) {
            case MEMBER: return IslandRole.MODERATOR;
            case MODERATOR: return IslandRole.ADMIN;
            default: return null;
        }
    }

    private IslandRole getPreviousRole(IslandRole current) {
        switch (current) {
            case ADMIN: return IslandRole.MODERATOR;
            case MODERATOR: return IslandRole.MEMBER;
            default: return null;
        }
    }
}
