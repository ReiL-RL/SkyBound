package me.reil.skybound.core.listener;

import me.reil.skybound.api.event.GeneratorProduceEvent;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.booster.BoosterManager;
import me.reil.skybound.core.generator.GeneratorManager;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;

/**
 * Listens for cobblestone/basalt generation and replaces with generator ores.
 */
public final class GeneratorListener implements Listener {

    private final IslandManager islandManager;
    private final GeneratorManager generatorManager;
    private final BoosterManager boosterManager;

    public GeneratorListener(IslandManager islandManager, GeneratorManager generatorManager, BoosterManager boosterManager) {
        this.islandManager = islandManager;
        this.generatorManager = generatorManager;
        this.boosterManager = boosterManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        Material newType = event.getNewState().getType();

        // Only intercept cobblestone and basalt generation
        if (newType != Material.COBBLESTONE && newType != Material.BASALT) return;

        Island island = islandManager.getIslandAt(block.getLocation());
        if (island == null) return;

        Material generated = generatorManager.rollMaterial(island);

        // Fire event for addons
        GeneratorProduceEvent produceEvent = new GeneratorProduceEvent(island, block.getLocation(), generated);
        Bukkit.getPluginManager().callEvent(produceEvent);

        if (produceEvent.isCancelled()) return;

        event.getNewState().setType(produceEvent.getMaterial());
    }
}
