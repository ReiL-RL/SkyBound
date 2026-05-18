package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.team.TeamManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.block.Block;

/**
 * Full island protection: blocks, containers, doors, buttons, entities, buckets, hanging entities.
 */
public final class IslandProtectionListener implements Listener {

    private final IslandManager islandManager;
    private final TeamManager teamManager;

    public IslandProtectionListener(IslandManager islandManager, TeamManager teamManager) {
        this.islandManager = islandManager;
        this.teamManager = teamManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!canBuild(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        // Containers, doors, buttons, levers, trapdoors, gates, etc.
        if (isInteractable(type)) {
            if (!canInteract(event.getPlayer(), block.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!canInteract(event.getPlayer(), entity.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();

        // Allow PvE against monsters anywhere
        if (target instanceof Monster) return;

        // Protect animals and other entities on islands
        if (!canBuild(player, target.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return;
        Player player = (Player) event.getRemover();
        if (!canBuild(player, event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean canBuild(Player player, Location location) {
        if (player.hasPermission("skybound.admin.bypass")) return true;
        Island island = islandManager.getIslandAt(location);
        if (island == null) return true; // Not on any island
        IslandRole role = island.getMemberRole(player.getUniqueId());
        return role.isAtLeast(IslandRole.MEMBER);
    }

    private boolean canInteract(Player player, Location location) {
        if (player.hasPermission("skybound.admin.bypass")) return true;
        Island island = islandManager.getIslandAt(location);
        if (island == null) return true;
        IslandRole role = island.getMemberRole(player.getUniqueId());
        return role.isAtLeast(IslandRole.TRUSTED);
    }

    private boolean isInteractable(Material type) {
        String name = type.name();
        return name.contains("CHEST") || name.contains("FURNACE") || name.contains("HOPPER")
                || name.contains("BARREL") || name.contains("SHULKER") || name.contains("DOOR")
                || name.contains("GATE") || name.contains("TRAPDOOR") || name.contains("BUTTON")
                || name.contains("LEVER") || name.contains("ANVIL") || name.contains("BREWING")
                || name.contains("ENCHANTING") || name.contains("BEACON") || name.contains("DISPENSER")
                || name.contains("DROPPER") || name.contains("NOTE_BLOCK") || name.contains("JUKEBOX")
                || name.contains("CAMPFIRE") || name.contains("BELL") || name.contains("GRINDSTONE")
                || name.contains("STONECUTTER") || name.contains("LOOM") || name.contains("CARTOGRAPHY")
                || name.contains("SMITHING") || name.contains("LECTERN") || name.contains("COMPOSTER");
    }
}
