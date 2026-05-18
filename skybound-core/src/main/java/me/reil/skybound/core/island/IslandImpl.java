package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import org.bukkit.Location;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of the Island interface.
 */
public final class IslandImpl implements Island {

    private final String id;
    private String name;
    private String description;
    private UUID owner;
    private final Map<UUID, IslandRole> members = new LinkedHashMap<UUID, IslandRole>();
    private int level;
    private long experience;
    private int radius;
    private final Location center;
    private Location home;
    private boolean locked;
    private final long createdAt;
    private final Map<String, Location> warps = new LinkedHashMap<String, Location>();
    private final Map<String, Object> settings = new LinkedHashMap<String, Object>();
    private double bankBalance;
    private double value;

    public IslandImpl(String id, UUID owner, Location center, int radius) {
        this.id = id;
        this.owner = owner;
        this.center = center;
        this.radius = radius;
        this.name = owner.toString().substring(0, 8);
        this.description = "";
        this.level = 1;
        this.experience = 0;
        this.locked = false;
        this.createdAt = System.currentTimeMillis();
        this.bankBalance = 0.0;
        this.value = 0.0;
        this.members.put(owner, IslandRole.OWNER);
    }

    @Override public String getId() { return id; }
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public String getDescription() { return description; }
    @Override public void setDescription(String description) { this.description = description; }
    @Override public UUID getOwner() { return owner; }
    @Override public void setOwner(UUID owner) { this.owner = owner; }

    @Override
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members.keySet());
    }

    @Override
    public boolean addMember(UUID playerId, IslandRole role) {
        if (members.containsKey(playerId)) return false;
        members.put(playerId, role);
        return true;
    }

    @Override
    public boolean removeMember(UUID playerId) {
        if (playerId.equals(owner)) return false;
        return members.remove(playerId) != null;
    }

    @Override
    public IslandRole getMemberRole(UUID playerId) {
        IslandRole role = members.get(playerId);
        return role == null ? IslandRole.VISITOR : role;
    }

    @Override
    public void setMemberRole(UUID playerId, IslandRole role) {
        if (members.containsKey(playerId)) {
            members.put(playerId, role);
        }
    }

    @Override public int getLevel() { return level; }
    @Override public void setLevel(int level) { this.level = level; }
    @Override public long getExperience() { return experience; }

    @Override
    public void addExperience(long amount) {
        this.experience += amount;
        // TODO: Check for level up
    }

    @Override
    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override public int getRadius() { return radius; }
    @Override public void setRadius(int radius) { this.radius = radius; }
    @Override public Location getCenter() { return center.clone(); }
    @Override public Location getHome() { return home == null ? center.clone() : home.clone(); }
    @Override public void setHome(Location location) { this.home = location; }
    @Override public boolean isLocked() { return locked; }
    @Override public void setLocked(boolean locked) { this.locked = locked; }
    @Override public long getCreatedAt() { return createdAt; }

    @Override
    public Map<String, Location> getWarps() {
        return Collections.unmodifiableMap(warps);
    }

    @Override
    public void setWarp(String name, Location location) {
        warps.put(name, location);
    }

    @Override
    public void removeWarp(String name) {
        warps.remove(name);
    }

    @Override
    public Map<String, Object> getSettings() {
        return Collections.unmodifiableMap(settings);
    }

    @Override
    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }

    @Override public double getBankBalance() { return bankBalance; }
    @Override public void setBankBalance(double amount) { this.bankBalance = amount; }

    @Override
    public boolean isWithinBounds(Location location) {
        if (location == null || center == null) return false;
        if (location.getWorld() == null || center.getWorld() == null) return false;
        // Check XZ bounds (ignore Y for island detection)
        double dx = Math.abs(location.getX() - center.getX());
        double dz = Math.abs(location.getZ() - center.getZ());
        return dx <= radius && dz <= radius;
    }
}
