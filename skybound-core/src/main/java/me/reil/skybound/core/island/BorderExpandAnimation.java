package me.reil.skybound.core.island;

import me.reil.skybound.api.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Plays a visual animation when an island's border expands (size upgrade).
 * Shows green particles expanding outward from the old border to the new border.
 */
public final class BorderExpandAnimation {

    private final JavaPlugin plugin;

    public BorderExpandAnimation(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Play the border expand animation for all online island members.
     * @param island the island
     * @param oldRadius the previous radius
     * @param newRadius the new radius
     */
    public void play(final Island island, final int oldRadius, final int newRadius) {
        final Location center = island.getCenter();
        if (center.getWorld() == null) return;

        // Play sound for all members
        for (UUID memberId : island.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.playSound(member.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                member.sendMessage("\u00a7a\u00a7lIsland border expanded! \u00a7e" + oldRadius + " \u00a77\u2192 \u00a7a" + newRadius);
            }
        }

        // Animate particles expanding from old to new border over 2 seconds
        final int steps = 20; // 20 ticks = 1 second
        for (int step = 0; step < steps; step++) {
            final int currentStep = step;
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    double progress = (double) currentStep / steps;
                    int currentRadius = oldRadius + (int) ((newRadius - oldRadius) * progress);
                    spawnBorderParticles(center, currentRadius);
                }
            }, step);
        }
    }

    private void spawnBorderParticles(Location center, int radius) {
        if (center.getWorld() == null) return;
        double y = center.getY() + 1;
        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        Particle.DustOptions dust = new Particle.DustOptions(Color.LIME, 1.5f);

        // Draw border outline with particles (every 2 blocks)
        for (int i = -radius; i <= radius; i += 2) {
            // North and South edges
            center.getWorld().spawnParticle(Particle.REDSTONE, cx + i + 0.5, y, cz - radius + 0.5, 1, dust);
            center.getWorld().spawnParticle(Particle.REDSTONE, cx + i + 0.5, y, cz + radius + 0.5, 1, dust);
            // East and West edges
            center.getWorld().spawnParticle(Particle.REDSTONE, cx - radius + 0.5, y, cz + i + 0.5, 1, dust);
            center.getWorld().spawnParticle(Particle.REDSTONE, cx + radius + 0.5, y, cz + i + 0.5, 1, dust);
        }
    }
}
