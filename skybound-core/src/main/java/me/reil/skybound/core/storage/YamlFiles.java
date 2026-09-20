package me.reil.skybound.core.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class YamlFiles {

    private YamlFiles() {
    }

    public static void saveAtomically(JavaPlugin plugin, YamlConfiguration cfg, File file, String label) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File temp = new File(parent == null ? new File(".") : parent, file.getName() + ".tmp");
        try {
            cfg.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailed) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + label + ": " + e.getMessage());
            if (temp.exists() && !temp.delete()) {
                plugin.getLogger().warning("Failed to delete temporary file: " + temp.getAbsolutePath());
            }
        }
    }
}
