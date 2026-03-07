package com.pn86.pngoldprospecting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class DataManager {
    private final JavaPlugin plugin;
    private final Map<String, ProspectingBlock> blocks = new LinkedHashMap<>();
    private final Random random = new Random();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll(boolean resetStateOnLoad) {
        blocks.clear();
        File dataDir = getDataDir();
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            plugin.getLogger().warning("无法创建 data 目录: " + dataDir.getAbsolutePath());
            return;
        }

        File[] files = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String id = cfg.getString("id");
            if (id == null || id.isBlank()) {
                continue;
            }

            String worldName = cfg.getString("location.world");
            World world = Bukkit.getWorld(worldName == null ? "" : worldName);
            if (world == null) {
                plugin.getLogger().warning("淘金方块 " + id + " 的世界不存在，跳过加载。");
                continue;
            }

            Location location = new Location(world, cfg.getInt("location.x"), cfg.getInt("location.y"), cfg.getInt("location.z"));
            Material skin = parseSkin(cfg.getString("skin", "sand")).orElse(Material.SUSPICIOUS_SAND);
            int resetTime = Math.max(0, cfg.getInt("reset-time-seconds", plugin.getConfig().getInt("defaults.reset-time-seconds", 300)));

            ProspectingBlock block = new ProspectingBlock(id, location, skin, resetTime);
            if (resetStateOnLoad) {
                block.setOpened(false);
                block.setOpenedAtMillis(0L);
            } else {
                block.setOpened(cfg.getBoolean("state.opened", false));
                block.setOpenedAtMillis(cfg.getLong("state.opened-at-millis", 0L));
                block.tickReset();
            }

            ConfigurationSection loots = cfg.getConfigurationSection("loots");
            if (loots != null) {
                for (String lootKey : loots.getKeys(false)) {
                    ConfigurationSection lootSec = loots.getConfigurationSection(lootKey);
                    if (lootSec == null) {
                        continue;
                    }
                    String itemId = lootSec.getString("item-id", lootKey);
                    ItemStack item = lootSec.getItemStack("item");
                    String command = lootSec.getString("command");
                    int weight = Math.max(0, lootSec.getInt("weight", 0));
                    if ((item == null || item.getType().isAir()) && (command == null || command.isBlank())) {
                        continue;
                    }
                    block.getLoots().put(lootKey, new LootEntry(itemId, item, weight, command));
                }
            }

            blocks.put(id, block);
            applyCurrentAppearance(block);
            saveBlock(block);
        }
    }

    public File getDataDir() {
        return new File(plugin.getDataFolder(), "data");
    }

    public Map<String, ProspectingBlock> getBlocks() {
        return blocks;
    }

    public ProspectingBlock getBlock(String id) {
        return blocks.get(id);
    }

    public boolean exists(String id) {
        return blocks.containsKey(id);
    }

    public boolean createBlock(String id, Location location, Material skin, int resetTime) {
        if (exists(id)) {
            return false;
        }
        ProspectingBlock block = new ProspectingBlock(id, location, skin, resetTime);
        blocks.put(id, block);
        applyCurrentAppearance(block);
        saveBlock(block);
        return true;
    }

    public boolean moveBlock(String id, Location location) {
        ProspectingBlock block = getBlock(id);
        if (block == null) {
            return false;
        }
        setBlockAir(block.getLocation());
        block.setLocation(location);
        applyCurrentAppearance(block);
        saveBlock(block);
        return true;
    }

    public boolean deleteBlock(String id) {
        ProspectingBlock block = blocks.remove(id);
        if (block == null) {
            return false;
        }
        setBlockAir(block.getLocation());
        File file = new File(getDataDir(), id + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("删除淘金数据文件失败: " + file.getName());
        }
        return true;
    }

    public boolean setSkin(String id, Material skin) {
        ProspectingBlock block = getBlock(id);
        if (block == null) {
            return false;
        }
        block.setSkin(skin);
        applyCurrentAppearance(block);
        saveBlock(block);
        return true;
    }

    public boolean setResetTime(String id, int seconds) {
        ProspectingBlock block = getBlock(id);
        if (block == null) {
            return false;
        }
        block.setResetTimeSeconds(Math.max(0, seconds));
        saveBlock(block);
        return true;
    }

    public boolean addLoot(String id, String lootId, ItemStack stack, int weight, String command) {
        ProspectingBlock block = getBlock(id);
        if (block == null) {
            return false;
        }

        ItemStack safeStack = null;
        if (stack != null && !stack.getType().isAir()) {
            safeStack = stack.clone();
        }
        if (safeStack == null && (command == null || command.isBlank())) {
            return false;
        }

        String entryKey = lootId + "-" + UUID.randomUUID().toString().substring(0, 8);
        block.getLoots().put(entryKey, new LootEntry(lootId, safeStack, Math.max(0, weight), command));
        saveBlock(block);
        return true;
    }

    public boolean removeLoot(String id, String lootIdOrEntryKey) {
        ProspectingBlock block = getBlock(id);
        if (block == null) {
            return false;
        }

        LootEntry removed = block.getLoots().remove(lootIdOrEntryKey);
        if (removed == null) {
            Iterator<Map.Entry<String, LootEntry>> iterator = block.getLoots().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, LootEntry> entry = iterator.next();
                if (entry.getValue().itemId().equalsIgnoreCase(lootIdOrEntryKey)) {
                    iterator.remove();
                    removed = entry.getValue();
                    break;
                }
            }
        }

        if (removed == null) {
            return false;
        }

        saveBlock(block);
        return true;
    }

    public Optional<LootEntry> rollLoot(ProspectingBlock block) {
        int total = block.getLoots().values().stream().mapToInt(entry -> Math.max(0, entry.weight())).sum();
        if (total <= 0) {
            return Optional.empty();
        }

        int value = random.nextInt(total) + 1;
        int cumulative = 0;
        for (LootEntry entry : block.getLoots().values()) {
            cumulative += Math.max(0, entry.weight());
            if (value <= cumulative) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public void saveBlock(ProspectingBlock block) {
        File file = new File(getDataDir(), block.getId() + ".yml");
        FileConfiguration cfg = new YamlConfiguration();
        cfg.set("id", block.getId());
        cfg.set("location.world", Objects.requireNonNull(block.getLocation().getWorld()).getName());
        cfg.set("location.x", block.getLocation().getBlockX());
        cfg.set("location.y", block.getLocation().getBlockY());
        cfg.set("location.z", block.getLocation().getBlockZ());
        cfg.set("skin", block.getSkin() == Material.SUSPICIOUS_GRAVEL ? "gravel" : "sand");
        cfg.set("reset-time-seconds", block.getResetTimeSeconds());
        cfg.set("state.opened", block.isOpened());
        cfg.set("state.opened-at-millis", block.getOpenedAtMillis());

        cfg.set("loots", null);
        for (Map.Entry<String, LootEntry> loot : block.getLoots().entrySet()) {
            String path = "loots." + loot.getKey();
            LootEntry entry = loot.getValue();
            cfg.set(path + ".item-id", entry.itemId());
            cfg.set(path + ".item", entry.itemStack());
            cfg.set(path + ".weight", entry.weight());
            cfg.set(path + ".command", entry.command());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("保存淘金数据失败 " + block.getId() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        Collection<ProspectingBlock> all = blocks.values();
        for (ProspectingBlock block : all) {
            saveBlock(block);
        }
    }

    public void applyCurrentAppearance(ProspectingBlock block) {
        Location loc = block.getLocation();
        if (loc.getWorld() == null) {
            return;
        }
        if (block.isOpened()) {
            loc.getBlock().setType(getOpenedMaterial(block.getSkin()), false);
        } else {
            loc.getBlock().setType(block.getSkin(), false);
        }
    }

    private void setBlockAir(Location loc) {
        if (loc.getWorld() == null) {
            return;
        }
        loc.getBlock().setType(Material.AIR, false);
    }

    public Material getOpenedMaterial(Material skin) {
        String path = skin == Material.SUSPICIOUS_GRAVEL ? "defaults.opened-block.gravel" : "defaults.opened-block.sand";
        String configured = plugin.getConfig().getString(path,
                skin == Material.SUSPICIOUS_GRAVEL ? "GRAVEL" : "SAND");
        if (configured == null || configured.isBlank()) {
            return skin == Material.SUSPICIOUS_GRAVEL ? Material.GRAVEL : Material.SAND;
        }
        try {
            return Material.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return skin == Material.SUSPICIOUS_GRAVEL ? Material.GRAVEL : Material.SAND;
        }
    }

    public Optional<Material> parseSkin(String input) {
        if (input == null) {
            return Optional.empty();
        }
        return switch (input.toLowerCase()) {
            case "sand" -> Optional.of(Material.SUSPICIOUS_SAND);
            case "gravel" -> Optional.of(Material.SUSPICIOUS_GRAVEL);
            default -> Optional.empty();
        };
    }

    public Material getCommandDisplayMaterial() {
        String configured = plugin.getConfig().getString("defaults.command-loot-display-item", "DIAMOND");
        if (configured == null || configured.isBlank()) {
            return Material.DIAMOND;
        }
        try {
            return Material.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Material.DIAMOND;
        }
    }

    public ProspectingBlock getByLocation(Location location) {
        for (ProspectingBlock block : blocks.values()) {
            Location loc = block.getLocation();
            if (loc.getWorld() == null || location.getWorld() == null) {
                continue;
            }
            if (loc.getWorld().getUID().equals(location.getWorld().getUID())
                    && loc.getBlockX() == location.getBlockX()
                    && loc.getBlockY() == location.getBlockY()
                    && loc.getBlockZ() == location.getBlockZ()) {
                return block;
            }
        }
        return null;
    }
}
