package me.reil.skybound.core.generator;

import me.reil.skybound.api.generator.GeneratorProvider;
import me.reil.skybound.api.generator.GeneratorTier;
import me.reil.skybound.api.island.Island;
import me.reil.skybound.core.config.CoreConfig;
import me.reil.skybound.core.island.IslandManager;
import me.reil.skybound.core.upgrade.UpgradeManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class GeneratorManager implements GeneratorProvider {

    private final JavaPlugin plugin;
    private final CoreConfig config;
    private final IslandManager islandManager;
    private final UpgradeManager upgradeManager;
    private final Map<String, GeneratorTierImpl> tiers = new LinkedHashMap<String, GeneratorTierImpl>();
    private final Random random = new Random();

    public GeneratorManager(JavaPlugin plugin, CoreConfig config, IslandManager islandManager, UpgradeManager upgradeManager) {
        this.plugin = plugin;
        this.config = config;
        this.islandManager = islandManager;
        this.upgradeManager = upgradeManager;
        loadTiers();
    }

    public void reload() {
        tiers.clear();
        loadTiers();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GeneratorTier> getTiers() {
        return Collections.unmodifiableCollection((Collection<? extends GeneratorTier>) (Collection<?>) tiers.values());
    }

    @Override
    public GeneratorTier getTier(String tierId) {
        return tiers.get(tierId);
    }

    @Override
    public GeneratorTier getIslandTier(Island island) {
        if (island == null) return tiers.values().iterator().next();
        int level = island.getLevel();
        GeneratorTierImpl best = null;
        for (GeneratorTierImpl tier : tiers.values()) {
            if (tier.getRequiredLevel() <= level) {
                if (best == null || tier.getRequiredLevel() > best.getRequiredLevel()) {
                    best = tier;
                }
            }
        }
        return best != null ? best : tiers.values().iterator().next();
    }

    @Override
    public Material rollMaterial(Island island) {
        GeneratorTier tier = getIslandTier(island);
        if (tier == null) return Material.COBBLESTONE;

        Map<Material, Double> distribution = tier.getDistribution();
        double totalWeight = 0.0;
        for (double weight : distribution.values()) {
            totalWeight += weight;
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (Map.Entry<Material, Double> entry : distribution.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        return Material.COBBLESTONE;
    }

    @Override
    public Map<Material, Double> getDistribution(Island island) {
        GeneratorTier tier = getIslandTier(island);
        return tier == null ? Collections.<Material, Double>emptyMap() : tier.getDistribution();
    }

    private void loadTiers() {
        File file = new File(plugin.getDataFolder(), "generators.yml");
        if (!file.exists()) {
            plugin.saveResource("generators.yml", false);
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection tiersSection = cfg.getConfigurationSection("tiers");
        if (tiersSection == null) return;

        for (String tierId : tiersSection.getKeys(false)) {
            ConfigurationSection ts = tiersSection.getConfigurationSection(tierId);
            if (ts == null) continue;

            String displayName = ts.getString("display-name", tierId);
            Material icon = Material.matchMaterial(ts.getString("icon", "COBBLESTONE"));
            if (icon == null) icon = Material.COBBLESTONE;
            int requiredLevel = ts.getInt("required-level", 1);

            Map<Material, Double> distribution = new LinkedHashMap<Material, Double>();
            ConfigurationSection distSection = ts.getConfigurationSection("distribution");
            if (distSection != null) {
                for (String matName : distSection.getKeys(false)) {
                    Material mat = Material.matchMaterial(matName);
                    if (mat != null) {
                        distribution.put(mat, distSection.getDouble(matName, 0.0));
                    }
                }
            }

            tiers.put(tierId, new GeneratorTierImpl(tierId, displayName, requiredLevel, distribution, icon));
        }

        plugin.getLogger().info("Loaded " + tiers.size() + " generator tiers.");
    }
}
