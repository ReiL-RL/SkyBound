package me.reil.skybound.api.island;

/**
 * Permissions that can be granted per-role on an island.
 * Inspired by IridiumSkyblock and SuperiorSkyblock2 permission systems.
 */
public enum IslandPermission {

    // Building
    BLOCK_PLACE,
    BLOCK_BREAK,
    BUCKET_USE,
    REDSTONE_INTERACT,

    // Containers
    OPEN_CHEST,
    OPEN_BARREL,
    OPEN_SHULKER,
    OPEN_FURNACE,
    OPEN_HOPPER,
    OPEN_BREWING,
    OPEN_ANVIL,
    OPEN_ENCHANTING,

    // Entities
    KILL_ANIMALS,
    KILL_MONSTERS,
    BREED_ANIMALS,
    SHEAR,
    LEASH,
    RIDE,

    // Island management
    INVITE,
    KICK,
    BAN,
    PROMOTE,
    DEMOTE,
    SET_HOME,
    SET_WARP,
    DELETE_WARP,
    CHANGE_NAME,
    CHANGE_DESCRIPTION,
    LOCK_ISLAND,
    CHANGE_BIOME,
    CHANGE_BORDER,
    CHANGE_SETTINGS,

    // Economy
    BANK_DEPOSIT,
    BANK_WITHDRAW,
    PURCHASE_UPGRADE,
    PURCHASE_BOOSTER,

    // Generators
    PLACE_GENERATOR,
    BREAK_GENERATOR,
    UPGRADE_GENERATOR,

    // Misc
    PORTAL_USE,
    FLY,
    TELEPORT_HOME,
    TELEPORT_WARP,
    ISLAND_REGEN
}
