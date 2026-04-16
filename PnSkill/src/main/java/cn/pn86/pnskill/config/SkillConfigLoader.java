package cn.pn86.pnskill.config;

import cn.pn86.pnskill.PnSkillPlugin;
import cn.pn86.pnskill.model.SkillDefinition;
import cn.pn86.pnskill.model.SkillModeDefinition;
import cn.pn86.pnskill.model.SkillTitleSetting;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SkillConfigLoader {
    private final PnSkillPlugin plugin;
    private final Map<String, SkillDefinition> skillMap = new LinkedHashMap<>();

    public SkillConfigLoader(PnSkillPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        skillMap.clear();

        File file = new File(plugin.getDataFolder(), "skill.yml");
        if (!file.exists()) {
            plugin.saveResource("skill.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection root = yaml.getConfigurationSection(key);
            if (root == null) {
                continue;
            }

            String id = key.toLowerCase(Locale.ROOT);
            String name = root.getString("name", key);
            SkillModeDefinition modeA = readMode(root, "a");
            SkillModeDefinition modeB = readMode(root, "b");
            List<String> banWorlds = root.getStringList("banworld").stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
            boolean clearItem = root.getBoolean("clearitem", false);
            SkillTitleSetting titleSetting = readTitleSetting(root.getConfigurationSection("title"));

            skillMap.put(id, new SkillDefinition(id, name, modeA, modeB, banWorlds, clearItem, titleSetting));
        }
    }

    public SkillDefinition getSkill(String skillId) {
        return skillMap.get(skillId.toLowerCase(Locale.ROOT));
    }

    public Set<String> getSkillIds() {
        return Collections.unmodifiableSet(skillMap.keySet());
    }

    public Collection<SkillDefinition> getSkills() {
        return Collections.unmodifiableCollection(skillMap.values());
    }

    private SkillModeDefinition readMode(ConfigurationSection root, String mode) {
        ConfigurationSection section = root.getConfigurationSection(mode);
        if (section == null) {
            return null;
        }
        String title = section.getString("title", mode);
        long cooldown = section.getLong("time", 0L);
        List<String> actions = new ArrayList<>(section.getStringList("action"));
        return new SkillModeDefinition(title, cooldown, actions);
    }

    private SkillTitleSetting readTitleSetting(ConfigurationSection titleSection) {
        if (titleSection == null) {
            return null;
        }
        long fadeIn = titleSection.getLong("fade-in", -1);
        long stay = titleSection.getLong("stay", -1);
        long fadeOut = titleSection.getLong("fade-out", -1);
        return new SkillTitleSetting(fadeIn, stay, fadeOut);
    }
}
