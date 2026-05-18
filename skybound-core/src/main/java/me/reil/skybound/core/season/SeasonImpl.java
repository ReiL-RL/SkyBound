package me.reil.skybound.core.season;

import me.reil.skybound.api.season.Season;

public final class SeasonImpl implements Season {

    private final int number;
    private final long startTime;
    private final int durationDays;

    public SeasonImpl(int number, long startTime, int durationDays) {
        this.number = number;
        this.startTime = startTime;
        this.durationDays = durationDays;
    }

    @Override
    public int getNumber() { return number; }

    @Override
    public long getStartTime() { return startTime; }

    @Override
    public long getEndTime() {
        return startTime + ((long) durationDays * 24L * 60L * 60L * 1000L);
    }

    @Override
    public int getDurationDays() { return durationDays; }

    @Override
    public boolean isActive() {
        return System.currentTimeMillis() < getEndTime();
    }
}
