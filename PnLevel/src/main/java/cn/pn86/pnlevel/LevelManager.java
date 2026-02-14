package cn.pn86.pnlevel;

import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {
    private final PnLevelPlugin plugin;

    public LevelManager(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public int getRequiredExpForLevel(int currentLevel) {
        ConfigurationSection custom = plugin.getConfig().getConfigurationSection("level-exp.custom");
        if (custom != null && custom.contains(String.valueOf(currentLevel))) {
            return Math.max(1, custom.getInt(String.valueOf(currentLevel), 100));
        }
        int base = plugin.getConfig().getInt("level-exp.default", 100);
        int increment = plugin.getConfig().getInt("level-exp.increment", 0);
        return Math.max(1, base + ((currentLevel - 1) * increment));
    }

    public String getLevelPrefix(int level) {
        ConfigurationSection tags = plugin.getConfig().getConfigurationSection("level-tags");
        if (tags == null) return "";
        for (String key : tags.getKeys(false)) {
            String[] split = key.split("-");
            if (split.length != 2) continue;
            int min = Integer.parseInt(split[0]);
            int max = Integer.parseInt(split[1]);
            if (level >= min && level <= max) {
                return tags.getString(key, "");
            }
        }
        return "";
    }

    public void addExp(PlayerLevelData data, Player player, int amount, boolean sendMessage) {
        if (amount <= 0) return;
        data.setExp(Math.min(999, data.getExp() + amount));
        boolean leveled = false;
        while (data.getLevel() < plugin.getMaxLevel()) {
            int required = getRequiredExpForLevel(data.getLevel());
            if (data.getExp() < required) break;
            data.setExp(data.getExp() - required);
            data.setLevel(data.getLevel() + 1);
            leveled = true;
            player.sendMessage(ColorUtil.component(plugin.msg("level-up")
                    .replace("%level%", String.valueOf(data.getLevel()))));
            player.sendMessage(ColorUtil.component(plugin.msg("gift-available")
                    .replace("%level%", String.valueOf(data.getLevel()))));
        }
        if (data.getLevel() >= plugin.getMaxLevel()) {
            data.setExp(Math.min(data.getExp(), getRequiredExpForLevel(plugin.getMaxLevel()) - 1));
        }
        if (sendMessage) {
            player.sendMessage(ColorUtil.component(plugin.msg("exp-add").replace("%exp%", String.valueOf(amount))));
            if (!leveled) {
                player.sendActionBar(ColorUtil.component(plugin.msg("exp-now")
                        .replace("%exp%", String.valueOf(data.getExp()))
                        .replace("%need%", String.valueOf(getRequiredExpForLevel(data.getLevel())))));
            }
        }
    }

    public void removeExp(PlayerLevelData data, int amount) {
        data.setExp(Math.max(0, data.getExp() - amount));
    }

    public List<PlayerLevelData> top() {
        List<PlayerLevelData> list = new ArrayList<>(plugin.getPlayerDataManager().all());
        list.sort((a, b) -> {
            int levelCompare = Integer.compare(b.getLevel(), a.getLevel());
            return levelCompare != 0 ? levelCompare : Integer.compare(b.getExp(), a.getExp());
        });
        return list;
    }
}
