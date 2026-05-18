package me.reil.skybound.core.mission;

import me.reil.skybound.api.mission.MissionCondition;
import me.reil.skybound.api.mission.MissionType;

public final class MissionConditionImpl implements MissionCondition {

    private final String id;
    private final MissionType type;
    private final String target;
    private final int amount;

    public MissionConditionImpl(String id, MissionType type, String target, int amount) {
        this.id = id;
        this.type = type;
        this.target = target != null ? target : "";
        this.amount = amount;
    }

    @Override public String getId() { return id; }
    @Override public MissionType getType() { return type; }
    @Override public String getTarget() { return target; }
    @Override public int getAmount() { return amount; }

    /**
     * Check if a given action matches this condition's filter.
     */
    public boolean matches(MissionType actionType, String actionTarget) {
        if (this.type != actionType) return false;
        if (this.target.isEmpty()) return true; // empty = any
        return this.target.equalsIgnoreCase(actionTarget);
    }
}
