package cn.pn86.pndeathmessage.config;

import cn.pn86.pndeathmessage.PnDeathMessagePlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;

public class NameConfigManager {

    private final PnDeathMessagePlugin plugin;
    private File attackFile;
    private File itemFile;
    private FileConfiguration attackConfig;
    private FileConfiguration itemConfig;

    public NameConfigManager(PnDeathMessagePlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.attackFile = new File(plugin.getDataFolder(), "attack.yml");
        this.itemFile = new File(plugin.getDataFolder(), "item.yml");

        this.attackConfig = YamlConfiguration.loadConfiguration(attackFile);
        this.itemConfig = YamlConfiguration.loadConfiguration(itemFile);

        ensureAllEntityNames();
        ensureAllMaterialNames();
        saveConfigs();
    }

    public String getAttackName(EntityType type) {
        if (type == null) {
            return getAttackNone();
        }
        return attackConfig.getString("names." + type.name(), type.name());
    }

    public String getItemName(Material material) {
        if (material == null) {
            return getItemNone();
        }
        return itemConfig.getString("names." + material.name(), material.name());
    }

    public String getAttackNone() {
        return attackConfig.getString("none", "未知来源");
    }

    public String getItemNone() {
        return itemConfig.getString("none", "空手");
    }

    private void ensureAllEntityNames() {
        if (!attackConfig.contains("none")) {
            attackConfig.set("none", "未知来源");
        }

        for (EntityType type : EntityType.values()) {
            String path = "names." + type.name();
            if (!attackConfig.contains(path)) {
                attackConfig.set(path, type.name());
            }
        }
    }

    private void ensureAllMaterialNames() {
        if (!itemConfig.contains("none")) {
            itemConfig.set("none", "空手");
        }

        for (Material material : Material.values()) {
            String path = "names." + material.name();
            if (!itemConfig.contains(path)) {
                itemConfig.set(path, material.name());
            }
        }
    }

    private void saveConfigs() {
        try {
            attackConfig.save(attackFile);
            itemConfig.save(itemFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("保存 attack.yml 或 item.yml 失败: " + exception.getMessage());
        }
    }
}
