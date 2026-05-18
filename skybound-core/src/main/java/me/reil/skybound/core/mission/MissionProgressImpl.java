package me.reil.skybound.core.mission;

import me.reil.skybound.api.mission.ConditionMode;
import me.reil.skybound.api.mission.MissionCondition;
import me.reil.skybound.api.mission.MissionProgress;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MissionProgressImpl implements MissionProgress {

    private final UUID playerId;
    private final String missionId;
    private final Map<String, Integer> conditionProgress;
    private boolean completed;
    private boolean claimed;
    private long completedAt;
    private long startedAt;
    private long lastCompletedAt;

    public MissionProgressImpl(UUID playerId, String missionId) {
        this.playerId = playerId;
        this.missionId = missionId;
        this.conditionProgress = new LinkedHashMap<String, Integer>();
        this.completed = false;
        this.claimed = false;
        this.completedAt = 0L;
        this.startedAt = 0L;
        this.lastCompletedAt = 0L;
    }

    public MissionProgressImpl(UUID playerId, String missionId, Map<String, Integer> progress,
                               boolean completed, boolean claimed, long completedAt,
                               long startedAt, long lastCompletedAt) {
        this.playerId = playerId;
        this.missionId = missionId;
        this.conditionProgress = progress != null ? new LinkedHashMap<String, Integer>(progress) : new LinkedHashMap<String, Integer>();
        this.completed = completed;
        this.claimed = claimed;
        this.completedAt = completedAt;
        this.startedAt = startedAt;
        this.lastCompletedAt = lastCompletedAt;
    }

    @Override public UUID getPlayerId() { return playerId; }
    @Override public String getMissionId() { return missionId; }

    @Override
    public Map<String, Integer> getConditionProgress() {
        return Collections.unmodifiableMap(conditionProgress);
    }

    @Override
    public int getConditionProgress(String conditionId) {
        Integer val = conditionProgress.get(conditionId);
        return val == null ? 0 : val;
    }

    @Override public boolean isCompleted() { return completed; }
    @Override public boolean isClaimed() { return claimed; }
    @Override public long getCompletedAt() { return completedAt; }
    @Override public long getStartedAt() { return startedAt; }
    @Override public long getLastCompletedAt() { return lastCompletedAt; }

    @Override
    public double getPercentage() {
        // This needs the mission definition to calculate properly
        // Will be calculated externally
        return completed ? 1.0 : 0.0;
    }

    /**
     * Calculate percentage based on mission conditions.
     */
    public double calculatePercentage(MissionImpl mission) {
        if (completed) return 1.0;
        List<MissionConditionImpl> conditions = mission.getConditionImpls();
        if (conditions.isEmpty()) return 1.0;

        if (mission.getConditionMode() == ConditionMode.OR) {
            // OR: best single condition percentage
            double best = 0.0;
            for (MissionConditionImpl cond : conditions) {
                int progress = getConditionProgress(cond.getId());
                double pct = cond.getAmount() <= 0 ? 1.0 : (double) progress / cond.getAmount();
                if (pct > best) best = pct;
            }
            return Math.min(1.0, best);
        } else {
            // AND: average of all conditions
            double total = 0.0;
            for (MissionConditionImpl cond : conditions) {
                int progress = getConditionProgress(cond.getId());
                double pct = cond.getAmount() <= 0 ? 1.0 : (double) Math.min(progress, cond.getAmount()) / cond.getAmount();
                total += pct;
            }
            return total / conditions.size();
        }
    }

    /**
     * Add progress to a specific condition.
     */
    public void addConditionProgress(String conditionId, int amount) {
        if (startedAt == 0L) {
            startedAt = System.currentTimeMillis();
        }
        Integer current = conditionProgress.get(conditionId);
        int newVal = (current == null ? 0 : current) + amount;
        conditionProgress.put(conditionId, newVal);
    }

    /**
     * Check if all/any conditions are met based on mode.
     */
    public boolean checkCompletion(MissionImpl mission) {
        List<MissionConditionImpl> conditions = mission.getConditionImpls();
        if (conditions.isEmpty()) return true;

        if (mission.getConditionMode() == ConditionMode.OR) {
            for (MissionConditionImpl cond : conditions) {
                int progress = getConditionProgress(cond.getId());
                if (progress >= cond.getAmount()) return true;
            }
            return false;
        } else {
            for (MissionConditionImpl cond : conditions) {
                int progress = getConditionProgress(cond.getId());
                if (progress < cond.getAmount()) return false;
            }
            return true;
        }
    }

    public void markCompleted() {
        this.completed = true;
        this.completedAt = System.currentTimeMillis();
        this.lastCompletedAt = this.completedAt;
    }

    public void markClaimed() {
        this.claimed = true;
    }

    /**
     * Reset for repeatable missions.
     */
    public void reset() {
        this.conditionProgress.clear();
        this.completed = false;
        this.claimed = false;
        this.completedAt = 0L;
        this.startedAt = 0L;
    }
}
