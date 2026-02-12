package com.pn86.pnvip.data;

import com.pn86.pnvip.PnVipPlugin;
import com.pn86.pnvip.model.PlayerVipRecord;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStore {
    private final PnVipPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerVipRecord> records = new HashMap<>();

    public DataStore(PnVipPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    public void load() {
        records.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String uuidKey : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(uuidKey);
            if (section == null) {
                continue;
            }
            PlayerVipRecord record = new PlayerVipRecord(section.getString("name", "unknown"));

            ConfigurationSection vips = section.getConfigurationSection("vips");
            if (vips != null) {
                for (String vipName : vips.getKeys(false)) {
                    record.getVipExpireAt().put(vipName.toLowerCase(Locale.ROOT), vips.getLong(vipName + ".expire-at", 0L));
                    String lastSignin = vips.getString(vipName + ".last-signin");
                    if (lastSignin != null && !lastSignin.isEmpty()) {
                        record.getLastSigninDate().put(vipName.toLowerCase(Locale.ROOT), lastSignin);
                    }
                }
            }
            records.put(uuid, record);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerVipRecord> entry : records.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerVipRecord record = entry.getValue();
            config.set(base + ".name", record.getName());
            for (Map.Entry<String, Long> vip : record.getVipExpireAt().entrySet()) {
                String vipBase = base + ".vips." + vip.getKey();
                config.set(vipBase + ".expire-at", vip.getValue());
                String lastSignin = record.getLastSigninDate().get(vip.getKey());
                if (lastSignin != null) {
                    config.set(vipBase + ".last-signin", lastSignin);
                }
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    public Set<UUID> getAllPlayers() {
        return records.keySet();
    }

    public PlayerVipRecord getRecord(UUID uuid) {
        return records.get(uuid);
    }

    public PlayerVipRecord getOrCreateRecord(UUID uuid, String playerName) {
        PlayerVipRecord record = records.computeIfAbsent(uuid, k -> new PlayerVipRecord(playerName));
        record.setName(playerName);
        return record;
    }
}
