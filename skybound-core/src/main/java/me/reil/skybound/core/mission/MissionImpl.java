package me.reil.skybound.core.mission;

import me.reil.skybound.api.mission.ConditionMode;
import me.reil.skybound.api.mission.Mission;
import me.reil.skybound.api.mission.MissionCondition;
import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

public final class MissionImpl implements Mission {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final List<MissionConditionImpl> conditions;
    private final ConditionMode conditionMode;
    private final double moneyReward;
    private final long xpReward;
    private final List<String> commandRewards;
    private final List<String> itemRewards;
    private final int requiredLevel;
    private final boolean repeatable;
    private final int cooldownSeconds;
    private final int timeLimitSeconds;
    private final List<String> prerequisites;
    private final Material icon;
    private final String category;
    private final String resetType;

    public MissionImpl(String id, String displayName, List<String> description,
                       List<MissionConditionImpl> conditions, ConditionMode conditionMode,
                       double moneyReward, long xpReward, List<String> commandRewards,
                       List<String> itemRewards, int requiredLevel, boolean repeatable,
                       int cooldownSeconds, int timeLimitSeconds, List<String> prerequisites,
                       Material icon, String category) {
        this(id, displayName, description, conditions, conditionMode, moneyReward, xpReward,
             commandRewards, itemRewards, requiredLevel, repeatable, cooldownSeconds,
             timeLimitSeconds, prerequisites, icon, category, null);
    }

    public MissionImpl(String id, String displayName, List<String> description,
                       List<MissionConditionImpl> conditions, ConditionMode conditionMode,
                       double moneyReward, long xpReward, List<String> commandRewards,
                       List<String> itemRewards, int requiredLevel, boolean repeatable,
                       int cooldownSeconds, int timeLimitSeconds, List<String> prerequisites,
                       Material icon, String category, String resetType) {
        this.id = id;
        this.displayName = displayName;
        this.description = description != null ? description : Collections.<String>emptyList();
        this.conditions = conditions != null ? conditions : Collections.<MissionConditionImpl>emptyList();
        this.conditionMode = conditionMode != null ? conditionMode : ConditionMode.AND;
        this.moneyReward = moneyReward;
        this.xpReward = xpReward;
        this.commandRewards = commandRewards != null ? commandRewards : Collections.<String>emptyList();
        this.itemRewards = itemRewards != null ? itemRewards : Collections.<String>emptyList();
        this.requiredLevel = requiredLevel;
        this.repeatable = repeatable;
        this.cooldownSeconds = cooldownSeconds;
        this.timeLimitSeconds = timeLimitSeconds;
        this.prerequisites = prerequisites != null ? prerequisites : Collections.<String>emptyList();
        this.icon = icon != null ? icon : Material.PAPER;
        this.category = category != null ? category : "general";
        this.resetType = resetType;
    }

    @Override public String getId() { return id; }
    @Override public String getDisplayName() { return displayName; }
    @Override public List<String> getDescription() { return description; }

    @Override
    @SuppressWarnings("unchecked")
    public List<MissionCondition> getConditions() {
        return Collections.unmodifiableList((List<? extends MissionCondition>) (List<?>) conditions);
    }

    public List<MissionConditionImpl> getConditionImpls() { return conditions; }

    @Override public ConditionMode getConditionMode() { return conditionMode; }
    @Override public double getMoneyReward() { return moneyReward; }
    @Override public long getXpReward() { return xpReward; }
    @Override public List<String> getCommandRewards() { return commandRewards; }
    @Override public List<String> getItemRewards() { return itemRewards; }
    @Override public int getRequiredLevel() { return requiredLevel; }
    @Override public boolean isRepeatable() { return repeatable; }
    @Override public int getCooldownSeconds() { return cooldownSeconds; }
    @Override public int getTimeLimitSeconds() { return timeLimitSeconds; }
    @Override public List<String> getPrerequisites() { return prerequisites; }
    @Override public Material getIcon() { return icon; }
    @Override public String getCategory() { return category; }
    @Override public String getResetType() { return resetType; }
}
