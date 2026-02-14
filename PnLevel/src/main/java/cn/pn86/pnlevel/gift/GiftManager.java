package cn.pn86.pnlevel.gift;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class GiftManager {
    private final PnLevelPlugin plugin;

    public GiftManager(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isClaimed(PlayerLevelData data, int level) {
        return level <= data.getLastClaimedLevel();
    }

    public boolean isClaimable(PlayerLevelData data, int level) {
        return level <= data.getLevel() && level > data.getLastClaimedLevel();
    }

    public int claimLevel(Player player, PlayerLevelData data, int level) {
        if (!isClaimable(data, level)) {
            return 0;
        }
        for (int i = data.getLastClaimedLevel() + 1; i <= level; i++) {
            executeGift(player, i);
        }
        data.setLastClaimedLevel(level);
        return level;
    }

    public int claimAll(Player player, PlayerLevelData data) {
        if (data.getLevel() <= data.getLastClaimedLevel()) {
            return 0;
        }
        int start = data.getLastClaimedLevel() + 1;
        int end = data.getLevel();
        for (int level = start; level <= end; level++) {
            executeGift(player, level);
        }
        data.setLastClaimedLevel(end);
        return end - start + 1;
    }

    private void executeGift(Player player, int level) {
        ConfigurationSection levelSection = plugin.getGiftConfig().getConfigurationSection("level." + level);
        String message;
        List<String> actions;
        if (levelSection != null) {
            message = levelSection.getString("message", "");
            actions = levelSection.getStringList("action");
        } else {
            ConfigurationSection all = plugin.getGiftConfig().getConfigurationSection("all");
            if (all == null) {
                return;
            }
            message = all.getString("message", "");
            actions = all.getStringList("action");
        }
        if (!message.isBlank()) {
            player.sendMessage(ColorUtil.component(message.replace("%level%", String.valueOf(level))));
        }
        for (String action : actions == null ? Collections.<String>emptyList() : actions) {
            String cmd = action.replace("%player_name%", player.getName()).replace("%level%", String.valueOf(level));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }
    }
}
