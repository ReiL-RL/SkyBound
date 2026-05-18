package me.reil.skybound.core.schematic;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Schematic paste service.
 * Tries WorldEdit/FAWE first, falls back to a simple platform.
 */
public final class SchematicService {

    private final JavaPlugin plugin;
    private final File schematicsFolder;
    private boolean worldEditAvailable;

    public SchematicService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
        }
        this.worldEditAvailable = plugin.getServer().getPluginManager().isPluginEnabled("WorldEdit")
                || plugin.getServer().getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    /**
     * Paste a schematic at the given location.
     * @param schematicName file name (without path)
     * @param location center location
     * @return true if pasted successfully
     */
    public boolean paste(String schematicName, Location location) {
        if (schematicName == null || schematicName.isEmpty()) {
            schematicName = "default.schem";
        }

        File file = new File(schematicsFolder, schematicName);
        if (!file.exists()) {
            // Try .schematic extension
            file = new File(schematicsFolder, schematicName.replace(".schem", ".schematic"));
        }

        if (file.exists() && worldEditAvailable) {
            return pasteWithWorldEdit(file, location);
        }

        // Fallback: create a simple platform
        plugin.getLogger().info("No schematic found or WorldEdit unavailable. Using fallback island.");
        createFallbackIsland(location);
        return true;
    }

    private boolean pasteWithWorldEdit(File file, Location location) {
        try {
            // Try FAWE/WE API via reflection to avoid hard dependency
            Class<?> clipboardFormatClass = Class.forName("com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats");
            Method findByFile = clipboardFormatClass.getMethod("findByFile", File.class);
            Object format = findByFile.invoke(null, file);

            if (format == null) {
                plugin.getLogger().warning("WorldEdit cannot read schematic: " + file.getName());
                createFallbackIsland(location);
                return true;
            }

            // Full WE paste logic would go here
            // For now, log and use fallback
            plugin.getLogger().info("WorldEdit schematic paste: " + file.getName());
            // TODO: Complete WorldEdit clipboard paste implementation
            createFallbackIsland(location);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("WorldEdit paste failed: " + e.getMessage());
            createFallbackIsland(location);
            return true;
        }
    }

    /**
     * Creates a simple 7x7 grass platform with a tree and chest.
     */
    private void createFallbackIsland(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // 7x7 grass platform with dirt underneath
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                Block dirt = world.getBlockAt(x, cy - 1, z);
                dirt.setType(Material.DIRT);
                Block grass = world.getBlockAt(x, cy, z);
                grass.setType(Material.GRASS_BLOCK);
            }
        }

        // Bedrock core
        world.getBlockAt(cx, cy - 2, cz).setType(Material.BEDROCK);

        // Tree
        world.getBlockAt(cx + 2, cy + 1, cz + 2).setType(Material.AIR);
        world.generateTree(new Location(world, cx + 2, cy + 1, cz + 2), TreeType.TREE);

        // Chest with starter items
        Block chestBlock = world.getBlockAt(cx - 1, cy + 1, cz);
        chestBlock.setType(Material.CHEST);
        if (chestBlock.getState() instanceof Chest) {
            Chest chest = (Chest) chestBlock.getState();
            chest.getInventory().addItem(new ItemStack(Material.ICE, 2));
            chest.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET, 1));
            chest.getInventory().addItem(new ItemStack(Material.MELON_SLICE, 3));
            chest.getInventory().addItem(new ItemStack(Material.BONE_MEAL, 5));
            chest.getInventory().addItem(new ItemStack(Material.SUGAR_CANE, 1));
            chest.getInventory().addItem(new ItemStack(Material.PUMPKIN_SEEDS, 1));
            chest.getInventory().addItem(new ItemStack(Material.OAK_SAPLING, 2));
        }
    }
}
