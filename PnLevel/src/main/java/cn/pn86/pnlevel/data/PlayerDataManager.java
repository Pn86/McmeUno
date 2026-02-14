package cn.pn86.pnlevel.data;

import cn.pn86.pnlevel.PnLevelPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerDataManager {
    private final PnLevelPlugin plugin;
    private final Map<UUID, PlayerLevelData> cache = new HashMap<>();
    private File file;
    private YamlConfiguration yaml;

    public PlayerDataManager(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) {
            plugin.saveResource("playerdata.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = players.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                PlayerLevelData data = new PlayerLevelData(
                        uuid,
                        section.getString("name", "Unknown"),
                        section.getInt("level", plugin.getInitialLevel()),
                        section.getInt("exp", 0)
                );
                data.setLastClaimedLevel(section.getInt("last-claimed-level", 0));
                ConfigurationSection daily = section.getConfigurationSection("daily-record");
                if (daily != null) {
                    for (String rewardKey : daily.getKeys(false)) {
                        data.getDailyRewardRecord().put(rewardKey, daily.getString(rewardKey, ""));
                    }
                }
                cache.put(uuid, data);
            } catch (Exception ignored) {
            }
        }
    }

    public PlayerLevelData getOrCreate(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid,
                u -> new PlayerLevelData(u, name, plugin.getInitialLevel(), plugin.getInitialExp()));
    }

    public PlayerLevelData getByName(String name) {
        for (PlayerLevelData data : cache.values()) {
            if (data.getLastName().equalsIgnoreCase(name)) {
                return data;
            }
        }
        return null;
    }

    public Collection<PlayerLevelData> all() {
        return cache.values();
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public void save() {
        if (yaml == null) {
            yaml = new YamlConfiguration();
        }
        yaml.set("players", null);
        for (PlayerLevelData data : cache.values()) {
            String path = "players." + data.getUuid();
            yaml.set(path + ".name", data.getLastName());
            yaml.set(path + ".level", data.getLevel());
            yaml.set(path + ".exp", data.getExp());
            yaml.set(path + ".last-claimed-level", data.getLastClaimedLevel());
            yaml.set(path + ".daily-record", data.getDailyRewardRecord());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save playerdata.yml: " + e.getMessage());
        }
    }
}
