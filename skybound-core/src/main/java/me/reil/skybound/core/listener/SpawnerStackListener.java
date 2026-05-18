package me.reil.skybound.core.listener;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Spawner stacking: placing a spawner on top of another of the same type
 * increases the stack count. Breaking gives back all spawners.
 * Stack count is stored in block metadata.
 */
public final class SpawnerStackListener implements Listener {

    private static final String META_KEY = "skybound_spawner_stack";
    private final JavaPlugin plugin;
    private final IslandManager islandManager;

    public SpawnerStackListener(JavaPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        Block placed = event.getBlock();
        Block against = event.getBlockAgainst();

        // Check if placing against an existing spawner of the same type
        if (against.getType() != Material.SPAWNER) return;

        CreatureSpawner placedSpawner = (CreatureSpawner) placed.getState();
        CreatureSpawner againstSpawner = (CreatureSpawner) against.getState();

        if (placedSpawner.getSpawnedType() != againstSpawner.getSpawnedType()) return;

        // Stack onto existing spawner
        event.setCancelled(true);

        int currentStack = getStackSize(against);
        int newStack = currentStack + 1;
        setStackSize(against, newStack);

        // Remove one spawner from hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ChatColor.GREEN + "" + newStack);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.SPAWNER) return;

        Block block = event.getBlock();
        int stackSize = getStackSize(block);

        if (stackSize > 1) {
            event.setDropItems(false);
            // Drop all spawners
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            EntityType type = spawner.getSpawnedType();

            for (int i = 0; i < stackSize; i++) {
                ItemStack spawnerItem = new ItemStack(Material.SPAWNER);
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), spawnerItem);
            }

            event.getPlayer().sendMessage(ChatColor.YELLOW + "" + stackSize);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Block block = event.getSpawner().getBlock();
        int stackSize = getStackSize(block);

        if (stackSize > 1) {
            // Spawn extra mobs based on stack size (stack - 1 extra)
            for (int i = 1; i < stackSize; i++) {
                block.getWorld().spawnEntity(
                        event.getLocation().add(Math.random() * 2 - 1, 0, Math.random() * 2 - 1),
                        event.getEntityType()
                );
            }
        }
    }

    private int getStackSize(Block block) {
        if (!block.hasMetadata(META_KEY)) return 1;
        List<MetadataValue> values = block.getMetadata(META_KEY);
        for (MetadataValue value : values) {
            if (value.getOwningPlugin() == plugin) {
                return value.asInt();
            }
        }
        return 1;
    }

    private void setStackSize(Block block, int size) {
        block.setMetadata(META_KEY, new FixedMetadataValue(plugin, size));
    }
}
