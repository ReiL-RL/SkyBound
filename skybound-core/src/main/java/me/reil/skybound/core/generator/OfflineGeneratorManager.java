package me.reil.skybound.core.generator;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Offline progress: generators accumulate resources while all island members are offline.
 * When a member logs in, they receive the accumulated items.
 *
 * Rate: 1 item per 5 minutes offline, capped at 64 items max.
 */
public final class OfflineGeneratorManager {

    private static final long INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes
    private static final int MAX_ITEMS = 64;

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final GeneratorManager generatorManager;
    // islandId -> last logout timestamp of last member
    private final Map<String, Long> lastAllOffline = new LinkedHashMap<String, Long>();

    public OfflineGeneratorManager(JavaPlugin plugin, IslandManager islandManager, GeneratorManager generatorManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.generatorManager = generatorManager;
    }

    /**
     * Called when the last member of an island goes offline.
     */
    public void onAllMembersOffline(String islandId) {
        lastAllOffline.put(islandId, System.currentTimeMillis());
    }

    /**
     * Called when a member logs in. Calculates and gives offline rewards.
     */
    public void onMemberLogin(Player player) {
        Island island = islandManager.getPlayerIsland(player.getUniqueId());
        if (island == null) return;

        Long offlineSince = lastAllOffline.get(island.getId());
        if (offlineSince == null) return;

        // Check if any other member is already online (if so, offline gen already claimed)
        for (UUID memberId : island.getMembers()) {
            if (!memberId.equals(player.getUniqueId())) {
                Player other = Bukkit.getPlayer(memberId);
                if (other != null && other.isOnline()) {
                    lastAllOffline.remove(island.getId());
                    return; // Someone else was already online
                }
            }
        }

        long elapsed = System.currentTimeMillis() - offlineSince;
        int itemCount = (int) Math.min(MAX_ITEMS, elapsed / INTERVAL_MS);

        if (itemCount <= 0) {
            lastAllOffline.remove(island.getId());
            return;
        }

        // Generate items based on island's generator tier
        List<ItemStack> rewards = new ArrayList<ItemStack>();
        for (int i = 0; i < itemCount; i++) {
            Material mat = generatorManager.rollMaterial(island);
            if (mat != Material.COBBLESTONE) { // Skip cobblestone for offline rewards
                rewards.add(new ItemStack(mat, 1));
            }
        }

        if (!rewards.isEmpty()) {
            // Merge stacks
            Map<Material, Integer> merged = new LinkedHashMap<Material, Integer>();
            for (ItemStack item : rewards) {
                Integer count = merged.get(item.getType());
                merged.put(item.getType(), (count == null ? 0 : count) + item.getAmount());
            }

            player.sendMessage("\u00a76\u00a7lOffline Generator Rewards:");
            for (Map.Entry<Material, Integer> entry : merged.entrySet()) {
                ItemStack stack = new ItemStack(entry.getKey(), entry.getValue());
                player.getInventory().addItem(stack);
                player.sendMessage("\u00a77  +" + entry.getValue() + " " + entry.getKey().name().toLowerCase().replace('_', ' '));
            }
        }

        lastAllOffline.remove(island.getId());
    }

    /**
     * Check if all members of an island are offline.
     */
    public boolean areAllMembersOffline(Island island) {
        for (UUID memberId : island.getMembers()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Long> getOfflineTimestamps() {
        return lastAllOffline;
    }

    public void setOfflineTimestamps(Map<String, Long> data) {
        lastAllOffline.clear();
        lastAllOffline.putAll(data);
    }
}
