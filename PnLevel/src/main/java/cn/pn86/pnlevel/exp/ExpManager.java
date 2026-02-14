package cn.pn86.pnlevel.exp;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class ExpManager {
    private final PnLevelPlugin plugin;
    private final List<ExpRule> rules = new ArrayList<>();
    private final Map<UUID, Integer> onlineSeconds = new HashMap<>();

    public ExpManager(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadRules() {
        rules.clear();
        FileConfiguration exp = plugin.getExpConfig();
        for (String key : exp.getKeys(false)) {
            ConfigurationSection section = exp.getConfigurationSection(key);
            if (section == null) continue;
            ExpRule.Type type;
            try {
                type = ExpRule.Type.valueOf(section.getString("type", "TIME").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                continue;
            }
            List<String> values = section.getStringList("int");
            int upExp = section.getInt("upexp", 0);
            rules.add(new ExpRule(key, type, values, upExp,
                    section.getString("message", ""),
                    section.getString("title", ""),
                    section.getString("subtitle", "")));
        }
    }

    public List<ExpRule> getRules() {
        return rules;
    }

    public void onJoin(Player player) {
        onlineSeconds.put(player.getUniqueId(), 0);
    }

    public void onQuit(Player player) {
        onlineSeconds.remove(player.getUniqueId());
    }

    public void tickTimeRules() {
        LocalTime now = LocalTime.now();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            int seconds = onlineSeconds.getOrDefault(player.getUniqueId(), 0) + 1;
            onlineSeconds.put(player.getUniqueId(), seconds);
            PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
            for (ExpRule rule : rules) {
                if (rule.getType() != ExpRule.Type.TIME) continue;
                for (String value : rule.getValues()) {
                    if (value.matches("\\d+")) {
                        int interval = Integer.parseInt(value);
                        if (interval > 0 && seconds % interval == 0) {
                            awardByRule(player, data, rule);
                        }
                    } else if (value.matches("\\d{1,2}:\\d{2}")) {
                        LocalTime target = LocalTime.parse(value.length() == 4 ? "0" + value : value);
                        if (target.getHour() == now.getHour() && target.getMinute() == now.getMinute() && now.getSecond() == 0) {
                            String stamp = LocalDate.now().toString();
                            String existing = data.getDailyRewardRecord().get(rule.getId());
                            if (!stamp.equals(existing)) {
                                data.getDailyRewardRecord().put(rule.getId(), stamp);
                                awardByRule(player, data, rule);
                            }
                        }
                    }
                }
            }
        }
    }

    public void triggerKill(Player player, String entityId) {
        triggerByTypeAndValue(player, ExpRule.Type.KILL, entityId);
    }

    public void triggerDestroy(Player player, String blockId) {
        triggerByTypeAndValue(player, ExpRule.Type.DESTROY, blockId);
    }

    public void triggerPlace(Player player, String blockId) {
        triggerByTypeAndValue(player, ExpRule.Type.PLACE, blockId);
    }

    private void triggerByTypeAndValue(Player player, ExpRule.Type type, String value) {
        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        for (ExpRule rule : rules) {
            if (rule.getType() == type && rule.getValues().stream().anyMatch(s -> s.equalsIgnoreCase(value))) {
                awardByRule(player, data, rule);
            }
        }
    }

    private void awardByRule(Player player, PlayerLevelData data, ExpRule rule) {
        plugin.getLevelManager().addExp(data, player, rule.getUpExp(), true);
        if (!rule.getMessage().isBlank()) {
            player.sendMessage(ColorUtil.component(rule.getMessage()));
        }
        if (!rule.getTitle().isBlank() || !rule.getSubtitle().isBlank()) {
            player.showTitle(net.kyori.adventure.title.Title.title(ColorUtil.component(rule.getTitle()), ColorUtil.component(rule.getSubtitle())));
        }
    }
}
