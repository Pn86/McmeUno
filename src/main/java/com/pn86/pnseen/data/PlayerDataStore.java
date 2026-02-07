package com.pn86.pnseen.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class PlayerDataStore {
    private final JavaPlugin plugin;
    private final File dataFile;
    private final Map<UUID, PlayerData> dataMap = new HashMap<>();

    public PlayerDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public PlayerData getOrCreate(UUID uuid) {
        return dataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public PlayerData get(UUID uuid) {
        return dataMap.get(uuid);
    }

    public PlayerData findByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (PlayerData data : dataMap.values()) {
            if (data.getName() != null && data.getName().toLowerCase(Locale.ROOT).equals(lower)) {
                return data;
            }
        }
        return null;
    }

    public Collection<PlayerData> getAll() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    public void load() {
        dataMap.clear();
        if (!dataFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = config.getConfigurationSection("players");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                PlayerData data = new PlayerData(uuid);
                data.setName(section.getString("name"));
                data.setTotalPlaytimeMillis(section.getLong("totalPlaytimeMillis"));
                data.setLastJoinMillis(section.getLong("lastJoinMillis"));
                data.setLastSeenMillis(section.getLong("lastSeenMillis"));
                data.setLastWorld(section.getString("lastWorld"));
                data.setLastX(section.getDouble("lastX"));
                data.setLastY(section.getDouble("lastY"));
                data.setLastZ(section.getDouble("lastZ"));
                data.setLastIp(section.getString("lastIp"));
                dataMap.put(uuid, data);
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("无法解析玩家UUID: " + key);
            }
        }
    }

    public void save() {
        FileConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("players");
        for (PlayerData data : dataMap.values()) {
            ConfigurationSection section = root.createSection(data.getUuid().toString());
            section.set("name", data.getName());
            section.set("totalPlaytimeMillis", data.getTotalPlaytimeMillis());
            section.set("lastJoinMillis", data.getLastJoinMillis());
            section.set("lastSeenMillis", data.getLastSeenMillis());
            section.set("lastWorld", data.getLastWorld());
            section.set("lastX", data.getLastX());
            section.set("lastY", data.getLastY());
            section.set("lastZ", data.getLastZ());
            section.set("lastIp", data.getLastIp());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存 data.yml: " + e.getMessage());
        }
    }
}
