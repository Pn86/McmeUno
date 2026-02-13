package cn.pn86.pnvipcommand;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<String, Long> cooldownSeconds = new HashMap<>();
    private final Map<String, Map<UUID, Long>> cooldownData = new HashMap<>();

    public CooldownManager(FileConfiguration config) {
        loadCooldown(config);
    }

    public void loadCooldown(FileConfiguration config) {
        cooldownSeconds.clear();
        cooldownSeconds.put("ptp", config.getLong("cooldowns.ptp", 30));
        cooldownSeconds.put("pgive", config.getLong("cooldowns.pgive", 60));
        cooldownSeconds.put("ptime", config.getLong("cooldowns.ptime", 60));
        cooldownSeconds.put("pweather", config.getLong("cooldowns.pweather", 60));
        cooldownSeconds.put("pexp", config.getLong("cooldowns.pexp", 120));
    }

    public long getRemaining(String action, UUID uuid) {
        long cd = cooldownSeconds.getOrDefault(action, 0L);
        if (cd <= 0) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long last = cooldownData
                .computeIfAbsent(action, k -> new HashMap<>())
                .getOrDefault(uuid, 0L);

        long expireAt = last + cd * 1000;
        if (now >= expireAt) {
            return 0;
        }
        return (expireAt - now + 999) / 1000;
    }

    public void markUsed(String action, UUID uuid) {
        cooldownData
                .computeIfAbsent(action, k -> new HashMap<>())
                .put(uuid, System.currentTimeMillis());
    }
}
