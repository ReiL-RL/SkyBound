package me.reil.skybound.api.booster;

/**
 * Types of boosters available.
 */
public enum BoosterType {

    /** Increases farming/crop growth speed. */
    FARMING,

    /** Increases XP gain. */
    EXPERIENCE,

    /** Increases mob spawning rate. */
    SPAWNER,

    /** Increases flight duration. */
    FLIGHT,

    /** Increases generator speed. */
    GENERATOR,

    /** Increases island XP gain. */
    ISLAND_XP,

    /** Potion effect booster. */
    POTION_EFFECT,

    /** Custom (for addons). */
    CUSTOM
}
