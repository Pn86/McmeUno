package cn.pn86.pnwarp.service;

import cn.pn86.pnwarp.model.Warp;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarpStorage {
    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration data;
    private final Map<String, Warp> warpsByName = new HashMap<>();

    public WarpStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!dataFile.exists()) {
            plugin.saveResource("data.yml", false);
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        warpsByName.clear();

        ConfigurationSection section = data.getConfigurationSection("warps");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection warpSection = section.getConfigurationSection(key);
            if (warpSection == null) {
                continue;
            }
            Optional<Warp> optionalWarp = parseWarp(warpSection);
            optionalWarp.ifPresent(warp -> warpsByName.put(normalize(warp.name()), warp));
        }
    }

    private Optional<Warp> parseWarp(ConfigurationSection section) {
        String name = section.getString("name", "");
        String desc = section.getString("description", "");
        String iconName = section.getString("icon", "COMPASS");
        Material icon = Material.matchMaterial(iconName);
        if (icon == null || !icon.isItem()) {
            icon = Material.COMPASS;
        }

        String ownerUuid = section.getString("owner-uuid");
        String ownerName = section.getString("owner-name", "Unknown");
        UUID owner;
        try {
            owner = ownerUuid == null ? UUID.randomUUID() : UUID.fromString(ownerUuid);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }

        String worldName = section.getString("location.world");
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            return Optional.empty();
        }

        double x = section.getDouble("location.x");
        double y = section.getDouble("location.y");
        double z = section.getDouble("location.z");
        float yaw = (float) section.getDouble("location.yaw");
        float pitch = (float) section.getDouble("location.pitch");

        Location location = new Location(world, x, y, z, yaw, pitch);
        return Optional.of(new Warp(name, desc, icon, owner, ownerName, location));
    }

    public Collection<Warp> all() {
        return warpsByName.values();
    }

    public Optional<Warp> getByName(String name) {
        return Optional.ofNullable(warpsByName.get(normalize(name)));
    }

    public boolean addWarp(Warp warp) {
        String key = normalize(warp.name());
        if (warpsByName.containsKey(key)) {
            return false;
        }
        warpsByName.put(key, warp);
        save();
        return true;
    }

    public boolean removeWarp(String name) {
        Warp removed = warpsByName.remove(normalize(name));
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public int removeByOwner(UUID owner) {
        int before = warpsByName.size();
        warpsByName.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
        int removed = before - warpsByName.size();
        if (removed > 0) {
            save();
        }
        return removed;
    }

    public int removeAll() {
        int count = warpsByName.size();
        warpsByName.clear();
        save();
        return count;
    }

    public void save() {
        data.set("warps", null);
        int i = 0;
        for (Warp warp : warpsByName.values()) {
            String base = "warps." + i++;
            data.set(base + ".name", warp.name());
            data.set(base + ".description", warp.description());
            data.set(base + ".icon", warp.icon().name());
            data.set(base + ".owner-uuid", warp.owner().toString());
            data.set(base + ".owner-name", warp.ownerName());
            data.set(base + ".location.world", warp.location().getWorld().getName());
            data.set(base + ".location.x", warp.location().getX());
            data.set(base + ".location.y", warp.location().getY());
            data.set(base + ".location.z", warp.location().getZ());
            data.set(base + ".location.yaw", warp.location().getYaw());
            data.set(base + ".location.pitch", warp.location().getPitch());
        }

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT);
    }
}
