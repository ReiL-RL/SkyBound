package me.reil.skybound.core.booster;

import me.reil.skybound.api.booster.ActiveBooster;

public final class ActiveBoosterImpl implements ActiveBooster {

    private final String boosterId;
    private final long activatedAt;
    private final long expiresAt;
    private final double multiplier;

    public ActiveBoosterImpl(String boosterId, long activatedAt, long expiresAt, double multiplier) {
        this.boosterId = boosterId;
        this.activatedAt = activatedAt;
        this.expiresAt = expiresAt;
        this.multiplier = multiplier;
    }

    @Override public String getBoosterId() { return boosterId; }
    @Override public long getActivatedAt() { return activatedAt; }
    @Override public long getExpiresAt() { return expiresAt; }

    @Override
    public long getRemainingSeconds() {
        return Math.max(0L, (expiresAt - System.currentTimeMillis()) / 1000L);
    }

    @Override
    public boolean isActive() {
        return System.currentTimeMillis() < expiresAt;
    }

    @Override public double getMultiplier() { return multiplier; }
}
