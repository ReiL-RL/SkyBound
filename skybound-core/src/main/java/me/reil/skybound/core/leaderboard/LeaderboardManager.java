package me.reil.skybound.core.leaderboard;

import me.reil.skybound.api.island.Island;
import me.reil.skybound.api.leaderboard.LeaderboardEntry;
import me.reil.skybound.api.leaderboard.LeaderboardProvider;
import me.reil.skybound.core.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Leaderboard/Top Islands implementation.
 */
public final class LeaderboardManager implements LeaderboardProvider {

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private List<LeaderboardEntryImpl> cachedByValue = new ArrayList<LeaderboardEntryImpl>();
    private List<LeaderboardEntryImpl> cachedByLevel = new ArrayList<LeaderboardEntryImpl>();

    public LeaderboardManager(JavaPlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LeaderboardEntry> getTopByValue(int limit) {
        if (cachedByValue.isEmpty()) recalculate();
        int end = Math.min(limit, cachedByValue.size());
        return Collections.unmodifiableList((List<? extends LeaderboardEntry>) (List<?>) cachedByValue.subList(0, end));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<LeaderboardEntry> getTopByLevel(int limit) {
        if (cachedByLevel.isEmpty()) recalculate();
        int end = Math.min(limit, cachedByLevel.size());
        return Collections.unmodifiableList((List<? extends LeaderboardEntry>) (List<?>) cachedByLevel.subList(0, end));
    }

    @Override
    public int getRankByValue(String islandId) {
        for (int i = 0; i < cachedByValue.size(); i++) {
            if (cachedByValue.get(i).getIslandId().equals(islandId)) return i + 1;
        }
        return -1;
    }

    @Override
    public int getRankByLevel(String islandId) {
        for (int i = 0; i < cachedByLevel.size(); i++) {
            if (cachedByLevel.get(i).getIslandId().equals(islandId)) return i + 1;
        }
        return -1;
    }

    @Override
    public void recalculate() {
        List<LeaderboardEntryImpl> entries = new ArrayList<LeaderboardEntryImpl>();
        for (Island island : islandManager.getAllIslands()) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
            String ownerName = owner.getName() != null ? owner.getName() : island.getOwner().toString().substring(0, 8);
            entries.add(new LeaderboardEntryImpl(island.getId(), island.getName(), island.getOwner(), ownerName,
                    island.getValue(), island.getLevel(), 0, island.getMembers().size()));
        }

        // Sort by value
        cachedByValue = new ArrayList<LeaderboardEntryImpl>(entries);
        Collections.sort(cachedByValue, new Comparator<LeaderboardEntryImpl>() {
            @Override
            public int compare(LeaderboardEntryImpl a, LeaderboardEntryImpl b) {
                return Double.compare(b.getValue(), a.getValue());
            }
        });
        for (int i = 0; i < cachedByValue.size(); i++) {
            cachedByValue.get(i).setRank(i + 1);
        }

        // Sort by level
        cachedByLevel = new ArrayList<LeaderboardEntryImpl>(entries);
        Collections.sort(cachedByLevel, new Comparator<LeaderboardEntryImpl>() {
            @Override
            public int compare(LeaderboardEntryImpl a, LeaderboardEntryImpl b) {
                return Integer.compare(b.getLevel(), a.getLevel());
            }
        });
        for (int i = 0; i < cachedByLevel.size(); i++) {
            cachedByLevel.get(i).setRank(i + 1);
        }
    }
}
