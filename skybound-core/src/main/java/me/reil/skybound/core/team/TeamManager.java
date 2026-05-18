package me.reil.skybound.core.team;

import me.reil.skybound.api.event.IslandMemberJoinEvent;
import me.reil.skybound.api.event.IslandMemberLeaveEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.api.team.TeamProvider;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Team management implementation.
 * Handles invites, kicks, promotions, co-op, and trust.
 */
public final class TeamManager implements TeamProvider {

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
        pendingInvites.put(target, island.getId());
        // TODO: Send message to target player
        return true;
    }

    @Override
    public boolean acceptInvite(UUID playerId, String islandId) {
        String pending = pendingInvites.get(playerId);
        if (pending == null || !pending.equals(islandId)) return false;

        Island island = islandManager.getIsland(islandId);
        if (island == null) return false;

        IslandMemberJoinEvent event = new IslandMemberJoinEvent(island, playerId, IslandRole.MEMBER);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        island.addMember(playerId, IslandRole.MEMBER);
        islandManager.registerMember(playerId, islandId);
        pendingInvites.remove(playerId);
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
        if (!kickerRole.isAtLeast(IslandRole.MODERATOR) || targetRole.isAtLeast(kickerRole)) return false;

        IslandMemberLeaveEvent event = new IslandMemberLeaveEvent(island, target, IslandMemberLeaveEvent.LeaveReason.KICKED);
        Bukkit.getPluginManager().callEvent(event);

        island.removeMember(target);
        return true;
    }

    @Override
    public boolean ban(Island island, UUID banner, UUID target) {
        if (island == null) return false;
        // TODO: Implement ban list
        kick(island, banner, target);
        return true;
    }

    @Override
    public boolean unban(Island island, UUID unbanner, UUID target) {
        // TODO: Implement unban
        return true;
    }

    @Override
    public boolean promote(Island island, UUID promoter, UUID target) {
        if (island == null || !island.getMembers().contains(target)) return false;
        IslandRole current = island.getMemberRole(target);
        IslandRole promoterRole = island.getMemberRole(promoter);

        if (current == IslandRole.OWNER || !promoterRole.isAtLeast(IslandRole.ADMIN)) return false;

        IslandRole next = getNextRole(current);
        if (next == null || next.isAtLeast(promoterRole)) return false;

        island.setMemberRole(target, next);
        return true;
    }

    @Override
    public boolean demote(Island island, UUID demoter, UUID target) {
        if (island == null || !island.getMembers().contains(target)) return false;
        IslandRole current = island.getMemberRole(target);
        IslandRole demoterRole = island.getMemberRole(demoter);

        if (current == IslandRole.MEMBER || !demoterRole.isAtLeast(IslandRole.ADMIN)) return false;
        if (current.isAtLeast(demoterRole)) return false;

        IslandRole prev = getPreviousRole(current);
        if (prev == null) return false;

        island.setMemberRole(target, prev);
        return true;
    }

    @Override
    public boolean transferOwnership(Island island, UUID currentOwner, UUID newOwner) {
        if (island == null || !island.getOwner().equals(currentOwner)) return false;
        if (!island.getMembers().contains(newOwner)) return false;

        island.setMemberRole(currentOwner, IslandRole.ADMIN);
        island.setMemberRole(newOwner, IslandRole.OWNER);
        island.setOwner(newOwner);
        return true;
    }

    @Override
    public boolean addCoop(Island island, UUID adder, UUID target) {
        if (island == null || island.getMembers().contains(target)) return false;
        island.addMember(target, IslandRole.COOP);
        return true;
    }

    @Override
    public boolean removeCoop(Island island, UUID remover, UUID target) {
        if (island == null) return false;
        if (island.getMemberRole(target) != IslandRole.COOP) return false;
        island.removeMember(target);
        return true;
    }

    @Override
    public boolean trust(Island island, UUID truster, UUID target) {
        if (island == null || island.getMembers().contains(target)) return false;
        island.addMember(target, IslandRole.TRUSTED);
        islandManager.registerMember(target, island.getId());
        return true;
    }

    @Override
    public boolean untrust(Island island, UUID untruster, UUID target) {
        if (island == null) return false;
        if (island.getMemberRole(target) != IslandRole.TRUSTED) return false;
        island.removeMember(target);
        return true;
    }

    @Override
    public boolean hasPendingInvite(UUID playerId, String islandId) {
        String pending = pendingInvites.get(playerId);
        return pending != null && pending.equals(islandId);
    }

    @Override
    public int getMaxTeamSize(Island island) {
        // TODO: Factor in upgrades
        return 4;
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
