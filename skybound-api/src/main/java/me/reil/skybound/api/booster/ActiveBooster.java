package me.reil.skybound.api.booster;

/**
 * Represents an active booster on an island.
 */
public interface ActiveBooster {

    /** Booster id. */
    String getBoosterId();

    /** Activation timestamp. */
    long getActivatedAt();

    /** Expiration timestamp. */
    long getExpiresAt();

    /** Remaining seconds. */
    long getRemainingSeconds();

    /** Whether the booster is still active. */
    boolean isActive();

    /** The multiplier value. */
    double getMultiplier();
}
