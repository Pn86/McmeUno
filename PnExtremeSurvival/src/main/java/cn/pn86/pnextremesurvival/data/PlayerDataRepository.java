package cn.pn86.pnextremesurvival.data;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class PlayerDataRepository {

    private final PnExtremeSurvivalPlugin plugin;
    private File dataFile;
    private YamlConfiguration data;

    public PlayerDataRepository(PnExtremeSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Failed to create plugin data directory: " + folder.getAbsolutePath());
        }

        this.dataFile = new File(folder, "database.yml");
        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile()) {
                    throw new IllegalStateException("Failed to create data file: " + dataFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create data file: " + dataFile.getAbsolutePath(), e);
            }
        }

        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public synchronized Optional<PlayerLifeData> load(UUID uuid) {
        if (data == null) {
            return Optional.empty();
        }

        ConfigurationSection section = data.getConfigurationSection("players." + uuid);
        if (section == null) {
            return Optional.empty();
        }

        return Optional.of(new PlayerLifeData(
                section.getDouble("max-health", 20.0),
                section.getBoolean("permanently-dead", false)
        ));
    }

    public synchronized void save(UUID uuid, String name, double maxHealth, boolean permanentlyDead) {
        if (data == null || dataFile == null) {
            return;
        }

        String path = "players." + uuid;
        data.set(path + ".name", name);
        data.set(path + ".max-health", maxHealth);
        data.set(path + ".permanently-dead", permanentlyDead);
        data.set(path + ".updated-at", System.currentTimeMillis());
        flush();
    }

    public synchronized void close() {
        flush();
    }

    private void flush() {
        if (data == null || dataFile == null) {
            return;
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save database.yml: " + e.getMessage());
        }
    }
}
