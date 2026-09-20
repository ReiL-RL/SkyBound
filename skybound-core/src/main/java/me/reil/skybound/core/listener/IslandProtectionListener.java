package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.island.IslandPermission;
import me.reil.skybound.api.island.IslandRole;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.island.IslandPermissionManager;
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
    private final IslandPermissionManager permissionManager;

    public IslandProtectionListener(IslandManager islandManager, TeamManager teamManager,
                                    IslandPermissionManager permissionManager) {
        this.islandManager = islandManager;
        this.teamManager = teamManager;
        this.permissionManager = permissionManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!hasIslandPermission(event.getPlayer(), event.getBlock().getLocation(), IslandPermission.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!hasIslandPermission(event.getPlayer(), event.getBlock().getLocation(), IslandPermission.BLOCK_PLACE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!hasIslandPermission(event.getPlayer(), event.getBlock().getLocation(), IslandPermission.BUCKET_USE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!hasIslandPermission(event.getPlayer(), event.getBlock().getLocation(), IslandPermission.BUCKET_USE)) {
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
        IslandPermission permission = getInteractPermission(type);
        if (permission != null) {
            if (!hasIslandPermission(event.getPlayer(), block.getLocation(), permission)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!hasIslandPermission(event.getPlayer(), entity.getLocation(), IslandPermission.RIDE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();

        if (target instanceof Monster) {
            if (!hasIslandPermission(player, target.getLocation(), IslandPermission.KILL_MONSTERS)) {
                event.setCancelled(true);
            }
            return;
        }

        IslandPermission permission = target instanceof Animals ? IslandPermission.KILL_ANIMALS : IslandPermission.BLOCK_BREAK;
        if (!hasIslandPermission(player, target.getLocation(), permission)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player)) return;
        Player player = (Player) event.getRemover();
        if (!hasIslandPermission(player, event.getEntity().getLocation(), IslandPermission.BLOCK_BREAK)) {
            event.setCancelled(true);
        }
    }

    private boolean hasIslandPermission(Player player, Location location, IslandPermission permission) {
        if (player.hasPermission("skybound.admin.bypass")) return true;
        Island island = islandManager.getIslandAt(location);
        if (island == null) return true; // Not on any island
        if (teamManager.isBanned(island, player.getUniqueId())) return false;
        return permissionManager.hasPermission(island, player.getUniqueId(), permission);
    }

    private IslandPermission getInteractPermission(Material type) {
        String name = type.name();
        if (name.contains("CHEST")) return IslandPermission.OPEN_CHEST;
        if (name.contains("BARREL")) return IslandPermission.OPEN_BARREL;
        if (name.contains("SHULKER")) return IslandPermission.OPEN_SHULKER;
        if (name.contains("FURNACE")) return IslandPermission.OPEN_FURNACE;
        if (name.contains("HOPPER")) return IslandPermission.OPEN_HOPPER;
        if (name.contains("BREWING")) return IslandPermission.OPEN_BREWING;
        if (name.contains("ANVIL")) return IslandPermission.OPEN_ANVIL;
        if (name.contains("ENCHANTING")) return IslandPermission.OPEN_ENCHANTING;
        if (name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR")
                || name.contains("BUTTON") || name.contains("LEVER") || name.contains("DISPENSER")
                || name.contains("DROPPER") || name.contains("NOTE_BLOCK") || name.contains("JUKEBOX")
                || name.contains("BELL")) {
            return IslandPermission.REDSTONE_INTERACT;
        }
        if (name.contains("BEACON") || name.contains("CAMPFIRE") || name.contains("GRINDSTONE")
                || name.contains("STONECUTTER") || name.contains("LOOM") || name.contains("CARTOGRAPHY")
                || name.contains("SMITHING") || name.contains("LECTERN") || name.contains("COMPOSTER")) {
            return IslandPermission.OPEN_CHEST;
        }
        return null;
    }
}
