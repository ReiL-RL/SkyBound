package me.reil.skybound.core.listener;

import me.reil.skybound.core.island.PlayerStatsManager;
import me.reil.skybound.core.island.PlayerStatsManager.Stat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Tracks per-player statistics for /is stats.
 */
public final class PlayerStatsListener implements Listener {

    private final PlayerStatsManager stats;

    public PlayerStatsListener(PlayerStatsManager stats) {
        this.stats = stats;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        stats.onPlayerJoin(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        stats.onPlayerQuit(e.getPlayer());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        stats.increment(e.getPlayer().getUniqueId(), Stat.BLOCKS_BROKEN, 1);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        stats.increment(e.getPlayer().getUniqueId(), Stat.BLOCKS_PLACED, 1);
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null) {
            stats.increment(e.getEntity().getKiller().getUniqueId(), Stat.MOBS_KILLED, 1);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        stats.increment(e.getEntity().getUniqueId(), Stat.DEATHS, 1);
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            stats.increment(e.getPlayer().getUniqueId(), Stat.FISH_CAUGHT, 1);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof org.bukkit.entity.Player) {
            int amount = e.getRecipe().getResult().getAmount();
            stats.increment(e.getWhoClicked().getUniqueId(), Stat.ITEMS_CRAFTED, amount);
        }
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent e) {
        stats.increment(e.getPlayer().getUniqueId(), Stat.ITEMS_SMELTED, e.getItemAmount());
    }

    @EventHandler
    public void onXp(PlayerExpChangeEvent e) {
        if (e.getAmount() > 0) {
            stats.increment(e.getPlayer().getUniqueId(), Stat.XP_GAINED, e.getAmount());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        // Only count when crossing a chunk boundary — keeps cost low
        if (e.getFrom().getChunk().equals(e.getTo().getChunk())) return;
        stats.increment(e.getPlayer().getUniqueId(), Stat.DISTANCE_WALKED, 16);
    }
}
