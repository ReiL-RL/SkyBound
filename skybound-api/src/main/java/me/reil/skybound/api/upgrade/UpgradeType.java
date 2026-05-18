package me.reil.skybound.api.upgrade;

/**
 * Types of island upgrades.
 * Inspired by IridiumSkyblock and SuperiorSkyblock2.
 */
public enum UpgradeType {

    /** Increases island border size. */
    ISLAND_SIZE,

    /** Increases max team members. */
    TEAM_SIZE,

    /** Increases generator speed. */
    GENERATOR_SPEED,

    /** Increases ore generator tier (better ores). */
    GENERATOR_TIER,

    /** Increases max warps. */
    WARP_LIMIT,

    /** Increases hopper speed/limit. */
    HOPPER_LIMIT,

    /** Increases entity limit on island. */
    ENTITY_LIMIT,

    /** Increases block limit for specific blocks. */
    BLOCK_LIMIT,

    /** Increases crop growth speed. */
    CROP_GROWTH,

    /** Increases spawner rate. */
    SPAWNER_RATE,

    /** Increases mob drop multiplier. */
    MOB_DROP,

    /** Increases XP multiplier. */
    XP_MULTIPLIER,

    /** Increases flight time/access. */
    FLIGHT,

    /** Custom upgrade type (for addons). */
    CUSTOM
}
