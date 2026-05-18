package me.reil.skybound.api.island;

import org.bukkit.Location;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player island with all its data.
 */
public interface Island {

    /** Unique island identifier. */
    String getId();

    /** Island display name (customizable by owner). */
    String getName();
    void setName(String name);

    /** Island description. */
    String getDescription();
    void setDescription(String description);

    /** Owner UUID. */
    UUID getOwner();
    void setOwner(UUID owner);

    /** All member UUIDs (including owner). */
    Set<UUID> getMembers();

    /** Add a member to the island. */
    boolean addMember(UUID playerId, IslandRole role);

    /** Remove a member from the island. */
    boolean removeMember(UUID playerId);

    /** Get a member's role. */
    IslandRole getMemberRole(UUID playerId);

    /** Set a member's role. */
    void setMemberRole(UUID playerId, IslandRole role);

    /** Island level (earned through missions/XP). */
    int getLevel();
    void setLevel(int level);

    /** Island experience points. */
    long getExperience();
    void addExperience(long amount);

    /** Island value (calculated from blocks). */
    double getValue();

    /** Island size radius. */
    int getRadius();
    void setRadius(int radius);

    /** Island center location. */
    Location getCenter();

    /** Island home/spawn location. */
    Location getHome();
    void setHome(Location location);

    /** Whether the island is locked (visitors cannot enter). */
    boolean isLocked();
    void setLocked(boolean locked);

    /** Island creation timestamp. */
    long getCreatedAt();

    /** Island warps. */
    Map<String, Location> getWarps();
    void setWarp(String name, Location location);
    void removeWarp(String name);

    /** Island settings (key-value). */
    Map<String, Object> getSettings();
    void setSetting(String key, Object value);

    /** Island bank balance. */
    double getBankBalance();
    void setBankBalance(double amount);

    /** Check if a location is within this island's boundaries. */
    boolean isWithinBounds(Location location);
}
