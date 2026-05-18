package me.reil.skybound.api.mission;

/**
 * Types of mission conditions players can complete.
 */
public enum MissionType {

    /** Break specific blocks. */
    BREAK_BLOCK,

    /** Place specific blocks. */
    PLACE_BLOCK,

    /** Kill specific mobs. */
    KILL_MOB,

    /** Craft specific items. */
    CRAFT_ITEM,

    /** Smelt specific items. */
    SMELT_ITEM,

    /** Brew specific potions. */
    BREW_POTION,

    /** Enchant items. */
    ENCHANT_ITEM,

    /** Catch fish. */
    FISH,

    /** Harvest crops. */
    HARVEST,

    /** Shear sheep. */
    SHEAR,

    /** Breed animals. */
    BREED,

    /** Tame animals. */
    TAME,

    /** Reach a certain island level. */
    ISLAND_LEVEL,

    /** Reach a certain island value. */
    ISLAND_VALUE,

    /** Deposit money to island bank. */
    BANK_DEPOSIT,

    /** Buy items from shop. */
    SHOP_BUY,

    /** Sell items to shop. */
    SHOP_SELL,

    /** Collect from generators. */
    GENERATOR_COLLECT,

    /** Pick up items. */
    PICKUP_ITEM,

    /** Eat food. */
    EAT,

    /** Walk/run distance. */
    WALK_DISTANCE,

    /** Gain XP orbs. */
    GAIN_XP,

    /** Custom (handled by addons). */
    CUSTOM
}
