package me.reil.skybound.core.listener;

import me.reil.skybound.api.mission.MissionType;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.mission.MissionManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

/**
 * Tracks all player actions for the flexible mission system.
 * Fires trackAction() on MissionManager for each relevant event.
 */
public final class MissionTrackingListener implements Listener {

    private final IslandManager islandManager;
    private final MissionManager missionManager;

    public MissionTrackingListener(IslandManager islandManager, MissionManager missionManager) {
        this.islandManager = islandManager;
        this.missionManager = missionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material material = block.getType();

        // Check if it's a crop at max age (harvest)
        if (isMatureCrop(block)) {
            missionManager.trackAction(player.getUniqueId(), MissionType.HARVEST, material.name(), 1);
        }

        missionManager.trackAction(player.getUniqueId(), MissionType.BREAK_BLOCK, material.name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material material = event.getBlock().getType();
        missionManager.trackAction(player.getUniqueId(), MissionType.PLACE_BLOCK, material.name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        String entityType = entity.getType().name();
        missionManager.trackAction(killer.getUniqueId(), MissionType.KILL_MOB, entityType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getRecipe().getResult() == null) return;

        Material material = event.getRecipe().getResult().getType();
        int amount = event.getRecipe().getResult().getAmount();
        missionManager.trackAction(player.getUniqueId(), MissionType.CRAFT_ITEM, material.name(), amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        Material material = event.getItemType();
        int amount = event.getItemAmount();
        missionManager.trackAction(player.getUniqueId(), MissionType.SMELT_ITEM, material.name(), amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        missionManager.trackAction(player.getUniqueId(), MissionType.FISH, "", 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        String entityType = event.getEntity().getType().name();
        missionManager.trackAction(player.getUniqueId(), MissionType.SHEAR, entityType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        Player player = (Player) event.getBreeder();
        String entityType = event.getEntity().getType().name();
        missionManager.trackAction(player.getUniqueId(), MissionType.BREED, entityType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player)) return;
        Player player = (Player) event.getOwner();
        String entityType = event.getEntity().getType().name();
        missionManager.trackAction(player.getUniqueId(), MissionType.TAME, entityType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Material material = event.getItem().getType();
        missionManager.trackAction(player.getUniqueId(), MissionType.ENCHANT_ITEM, material.name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material material = event.getItem().getType();
        missionManager.trackAction(player.getUniqueId(), MissionType.EAT, material.name(), 1);
    }

    private boolean isMatureCrop(Block block) {
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }
}
