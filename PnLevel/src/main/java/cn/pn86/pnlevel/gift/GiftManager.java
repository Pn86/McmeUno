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
        return data.getClaimedLevels().contains(level);
    }

    public boolean isClaimable(PlayerLevelData data, int level) {
        return level <= data.getLevel() && !isClaimed(data, level);
    }

    public boolean claimLevel(Player player, PlayerLevelData data, int level) {
        if (!isClaimable(data, level)) {
            return false;
        }
        executeGift(player, level);
        data.getClaimedLevels().add(level);
        if (level > data.getLastClaimedLevel()) {
            data.setLastClaimedLevel(level);
        }
        return true;
    }

    public int claimAll(Player player, PlayerLevelData data) {
        int claimed = 0;
        for (int level = 1; level <= data.getLevel(); level++) {
            if (isClaimable(data, level) && claimLevel(player, data, level)) {
                claimed++;
            }
        }
        return claimed;
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
            String parsed = plugin.applyPapi(player, message.replace("%level%", String.valueOf(level)));
            player.sendMessage(ColorUtil.component(parsed));
        }
        for (String action : actions == null ? Collections.<String>emptyList() : actions) {
            String cmd = plugin.applyPapi(player, action.replace("%player_name%", player.getName()).replace("%level%", String.valueOf(level)));
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }
    }
}
