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
import java.util.List;

public class ConfigService {

    public record LoadResult(FileConfiguration configuration, List<MineRegion> mines) {
    }

    public LoadResult load(File file) throws ConfigValidationException {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection mineSection = cfg.getConfigurationSection("mine");
        if (mineSection == null || mineSection.getKeys(false).isEmpty()) {
            throw new ConfigValidationException("缺少 mine 配置或 mine 为空");
        }

        List<MineRegion> mines = new ArrayList<>();
        for (String mineName : mineSection.getKeys(false)) {
            String base = "mine." + mineName;
            String world = requireString(cfg, base + ".world");

            int ax = requireInt(cfg, base + ".xyz.a.x");
            int ay = requireInt(cfg, base + ".xyz.a.y");
            int az = requireInt(cfg, base + ".xyz.a.z");
            int bx = requireInt(cfg, base + ".xyz.b.x");
            int by = requireInt(cfg, base + ".xyz.b.y");
            int bz = requireInt(cfg, base + ".xyz.b.z");

            int minX = Math.min(ax, bx);
            int minY = Math.min(ay, by);
            int minZ = Math.min(az, bz);
            int maxX = Math.max(ax, bx);
            int maxY = Math.max(ay, by);
            int maxZ = Math.max(az, bz);

            int time = Integer.parseInt(requireString(cfg, base + ".time"));
            if (time <= 0) {
                throw new ConfigValidationException(base + ".time 必须 > 0");
            }

            DropMode dropMode = DropMode.fromConfig(requireString(cfg, base + ".drop"));
            List<String> oreLines = cfg.getStringList(base + ".ore");
            if (oreLines.isEmpty()) {
                throw new ConfigValidationException(base + ".ore 不能为空");
            }

            List<OreEntry> ores = new ArrayList<>();
            for (String line : oreLines) {
                String[] split = line.split(":");
                if (split.length != 3) {
                    throw new ConfigValidationException(base + ".ore 格式错误: " + line);
                }
                Material material = Material.matchMaterial(split[0]);
                if (material == null || !material.isBlock()) {
                    throw new ConfigValidationException(base + ".ore 方块无效: " + split[0]);
                }
                int weight;
                double amount;
                try {
                    weight = Integer.parseInt(split[1]);
                    amount = Double.parseDouble(split[2]);
                } catch (NumberFormatException ex) {
                    throw new ConfigValidationException(base + ".ore 数值错误: " + line);
                }
                if (weight <= 0 || amount < 0) {
                    throw new ConfigValidationException(base + ".ore 权重需>0，数量/收益需>=0: " + line);
                }
                ores.add(new OreEntry(material, weight, amount));
            }

            mines.add(new MineRegion(mineName, world, minX, minY, minZ, maxX, maxY, maxZ, time, dropMode, ores));
        }
        return new LoadResult(cfg, mines);
    }

    public void backup(File source, File target) throws IOException {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(source);
        cfg.save(target);
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
}
