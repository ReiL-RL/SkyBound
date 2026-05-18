package me.reil.skybound.api.island;

/**
 * Roles within an island team.
 * Ordered from lowest to highest privilege.
 */
public enum IslandRole {

    VISITOR(0),
    COOP(1),
    TRUSTED(2),
    MEMBER(3),
    MODERATOR(4),
    ADMIN(5),
    OWNER(6);

    private final int weight;

    IslandRole(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isAtLeast(IslandRole other) {
        return this.weight >= other.weight;
    }
}
