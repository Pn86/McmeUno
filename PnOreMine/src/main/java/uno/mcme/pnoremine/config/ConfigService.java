package uno.mcme.pnoremine.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import uno.mcme.pnoremine.mine.DropMode;
import uno.mcme.pnoremine.mine.MineRegion;
import uno.mcme.pnoremine.mine.OreEntry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ConfigService {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    public record LoadResult(FileConfiguration configuration, List<MineRegion> mines) {
    }

    public LoadResult load(File file) throws ConfigValidationException {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection mineSection = cfg.getConfigurationSection("mine");
        if (mineSection == null || mineSection.getKeys(false).isEmpty()) {
            throw new ConfigValidationException("缺少 mine 配置或 mine 为空");
        }

        List<MineRegion> mines = new ArrayList<>();
        Map<String, String> worldConstraintCheck = new HashMap<>();
        for (String mineName : mineSection.getKeys(false)) {
            if (!NAME_PATTERN.matcher(mineName).matches()) {
                throw new ConfigValidationException("矿场名仅可使用字母和数字: " + mineName);
            }

            String base = "mine." + mineName;
            String world = requireString(cfg, base + ".world");

            int[] posA = parsePosition(cfg, base, "a", "pos1");
            int[] posB = parsePosition(cfg, base, "b", "pos2");
            int[] spawn = parseSimplePosition(requireString(cfg, base + ".spawn"), base + ".spawn");

            int minX = Math.min(posA[0], posB[0]);
            int minY = Math.min(posA[1], posB[1]);
            int minZ = Math.min(posA[2], posB[2]);
            int maxX = Math.max(posA[0], posB[0]);
            int maxY = Math.max(posA[1], posB[1]);
            int maxZ = Math.max(posA[2], posB[2]);

            int time = parseInt(requireString(cfg, base + ".time"), base + ".time");
            if (time <= 0) {
                throw new ConfigValidationException(base + ".time 必须 > 0");
            }

            boolean pvp = cfg.getBoolean(base + ".pvp", false);
            DropMode dropMode = DropMode.fromConfig(requireString(cfg, base + ".drop"));
            List<String> oreLines = cfg.getStringList(base + ".ore");
            if (oreLines.isEmpty()) {
                throw new ConfigValidationException(base + ".ore 不能为空");
            }

            List<OreEntry> ores = new ArrayList<>();
            for (String line : oreLines) {
                String normalized = line.trim().replace(':', ' ');
                String[] split = normalized.split("\\s+");
                if (split.length != 3) {
                    throw new ConfigValidationException(base + ".ore 格式错误: " + line);
                }
                Material material = Material.matchMaterial(split[0]);
                if (material == null || !material.isBlock()) {
                    throw new ConfigValidationException(base + ".ore 方块无效: " + split[0]);
                }
                int weight = parseInt(split[1], base + ".ore 权重");
                double amount = parseDouble(split[2], base + ".ore 数值");
                if (weight <= 0 || amount < 0) {
                    throw new ConfigValidationException(base + ".ore 权重需>0，数量/收益需>=0: " + line);
                }
                ores.add(new OreEntry(material, weight, amount));
            }

            String currentWorldRule = spawn[0] + "," + spawn[1] + "," + spawn[2] + "," + pvp;
            String existingRule = worldConstraintCheck.putIfAbsent(world.toLowerCase(), currentWorldRule);
            if (existingRule != null && !existingRule.equals(currentWorldRule)) {
                throw new ConfigValidationException("同一世界的 spawn/pvp 必须一致: " + world);
            }

            mines.add(new MineRegion(mineName, world, minX, minY, minZ, maxX, maxY, maxZ, time, dropMode, ores,
                spawn[0], spawn[1], spawn[2], pvp));
        }
        return new LoadResult(cfg, mines);
    }

    public void backup(File source, File target) throws IOException {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(source);
        cfg.save(target);
    }

    private int[] parsePosition(FileConfiguration cfg, String base, String oldNode, String simpleNode) throws ConfigValidationException {
        String simplePath = base + "." + simpleNode;
        if (cfg.isString(simplePath)) {
            return parseSimplePosition(requireString(cfg, simplePath), simplePath);
        }
        return new int[]{
            requireInt(cfg, base + ".xyz." + oldNode + ".x"),
            requireInt(cfg, base + ".xyz." + oldNode + ".y"),
            requireInt(cfg, base + ".xyz." + oldNode + ".z")
        };
    }

    private int[] parseSimplePosition(String value, String path) throws ConfigValidationException {
        String[] split = value.trim().split("\\s+");
        if (split.length != 3) {
            throw new ConfigValidationException(path + " 坐标格式错误，应为: x y z");
        }
        return new int[]{parseInt(split[0], path), parseInt(split[1], path), parseInt(split[2], path)};
    }

    private String requireString(FileConfiguration cfg, String path) throws ConfigValidationException {
        String val = cfg.getString(path);
        if (val == null || val.isBlank()) {
            throw new ConfigValidationException("缺少或为空: " + path);
        }
        return val;
    }

    private int requireInt(FileConfiguration cfg, String path) throws ConfigValidationException {
        if (!cfg.contains(path)) {
            throw new ConfigValidationException("缺少: " + path);
        }
        return cfg.getInt(path);
    }

    private int parseInt(String value, String path) throws ConfigValidationException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ConfigValidationException(path + " 必须为整数");
        }
    }

    private double parseDouble(String value, String path) throws ConfigValidationException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ConfigValidationException(path + " 必须为数值");
        }
    }
}
