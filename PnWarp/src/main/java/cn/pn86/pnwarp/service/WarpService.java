package cn.pn86.pnwarp.service;

import cn.pn86.pnwarp.model.Warp;
import cn.pn86.pnwarp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class WarpService {
    private final JavaPlugin plugin;
    private final WarpStorage storage;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public WarpService(JavaPlugin plugin, WarpStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void loadDefaultsIfNeeded() {
        if (!storage.all().isEmpty()) {
            return;
        }
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("default-warps");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String worldName = entry.getString("world");
            if (worldName == null || Bukkit.getWorld(worldName) == null) {
                continue;
            }
            String name = entry.getString("name", key);
            String description = entry.getString("description", "");
            Material icon = Material.matchMaterial(entry.getString("icon", "COMPASS"));
            if (icon == null || !icon.isItem()) {
                icon = Material.COMPASS;
            }
            UUID owner = UUID.nameUUIDFromBytes("SYSTEM".getBytes());
            Location location = new Location(
                    Bukkit.getWorld(worldName),
                    entry.getDouble("x"),
                    entry.getDouble("y"),
                    entry.getDouble("z"),
                    (float) entry.getDouble("yaw"),
                    (float) entry.getDouble("pitch")
            );
            storage.addWarp(new Warp(name, description, icon, owner, "SYSTEM", location));
        }
    }

    public int maxWarpsPerPlayer() {
        return plugin.getConfig().getInt("max-warps-per-player", 5);
    }

    public List<Warp> allWarpsSorted() {
        List<Warp> warps = new ArrayList<>(storage.all());
        warps.sort(Comparator.comparing(Warp::name, String.CASE_INSENSITIVE_ORDER));
        return warps;
    }

    public int warpCountOf(UUID playerId) {
        int count = 0;
        for (Warp warp : storage.all()) {
            if (warp.owner().equals(playerId)) {
                count++;
            }
        }
        return count;
    }

    public Optional<String> addPlayerWarp(Player player, String name, String description, String iconName) {
        if (storage.getByName(name).isPresent()) {
            return Optional.of(msg("messages.warp-exists"));
        }
        if (warpCountOf(player.getUniqueId()) >= maxWarpsPerPlayer()) {
            return Optional.of(msg("messages.max-warps-reached")
                    .replace("{max}", String.valueOf(maxWarpsPerPlayer())));
        }

        Material icon = Material.matchMaterial(iconName.toUpperCase(Locale.ROOT));
        if (icon == null || !icon.isItem()) {
            return Optional.of(msg("messages.invalid-icon"));
        }

        Warp warp = new Warp(name, description, icon, player.getUniqueId(), player.getName(), player.getLocation().clone());
        storage.addWarp(warp);
        return Optional.empty();
    }

    public Optional<Warp> getWarp(String name) {
        return storage.getByName(name);
    }

    public boolean removeWarpOwnedBy(Player player, String name) {
        Optional<Warp> warp = storage.getByName(name);
        if (warp.isEmpty()) {
            return false;
        }
        if (!warp.get().owner().equals(player.getUniqueId()) && !player.hasPermission("pnwarp.admin")) {
            return false;
        }
        return storage.removeWarp(name);
    }

    public boolean removeWarpAny(String name) {
        return storage.removeWarp(name);
    }

    public int removePlayerWarps(UUID uuid) {
        return storage.removeByOwner(uuid);
    }

    public int removeAllWarps() {
        return storage.removeAll();
    }

    public void scheduleTeleport(Player player, Warp warp) {
        cancelTeleport(player);

        int waitSeconds = Math.max(0, plugin.getConfig().getInt("teleport.wait-seconds", 3));
        if (waitSeconds == 0) {
            player.teleport(warp.location());
            playSound(player, "teleport.success-sound", Sound.ENTITY_ENDERMAN_TELEPORT);
            player.sendMessage(msg("messages.teleport-success").replace("{warp}", warp.name()));
            return;
        }

        Location start = player.getLocation().clone();
        player.sendMessage(msg("messages.teleport-start")
                .replace("{warp}", warp.name())
                .replace("{seconds}", String.valueOf(waitSeconds)));
        playSound(player, "teleport.start-sound", Sound.BLOCK_NOTE_BLOCK_PLING);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            if (!isSameBlock(player.getLocation(), start)) {
                player.sendMessage(msg("messages.teleport-cancelled"));
                playSound(player, "teleport.cancel-sound", Sound.BLOCK_NOTE_BLOCK_BASS);
                return;
            }
            player.teleport(warp.location());
            player.sendMessage(msg("messages.teleport-success").replace("{warp}", warp.name()));
            playSound(player, "teleport.success-sound", Sound.ENTITY_ENDERMAN_TELEPORT);
        }, waitSeconds * 20L);

        pendingTeleports.put(player.getUniqueId(), task);
    }

    public void cancelTeleport(Player player) {
        BukkitTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (BukkitTask task : pendingTeleports.values()) {
            task.cancel();
        }
        pendingTeleports.clear();
        storage.save();
    }

    private boolean isSameBlock(Location a, Location b) {
        return a.getWorld() != null && a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private void playSound(Player player, String path, Sound fallback) {
        String name = plugin.getConfig().getString(path, fallback.name());
        try {
            Sound sound = Sound.valueOf(name);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {
            player.playSound(player.getLocation(), fallback, 1f, 1f);
        }
    }

    public String msg(String path) {
        return TextUtil.color(plugin.getConfig().getString(path, ""));
    }
}
