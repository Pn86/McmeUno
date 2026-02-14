package cn.pn86.pnlevel.command;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCommands implements CommandExecutor {
    private final PnLevelPlugin plugin;

    public PlayerCommands(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        if (command.getName().equalsIgnoreCase("level")) {
            int need = plugin.getLevelManager().getRequiredExpForLevel(data.getLevel());
            player.sendMessage(ColorUtil.component(plugin.msg("self-info")
                    .replace("%level%", String.valueOf(data.getLevel()))
                    .replace("%exp%", String.valueOf(data.getExp()))
                    .replace("%need%", String.valueOf(Math.max(0, need - data.getExp())))));
            return true;
        }
        if (command.getName().equalsIgnoreCase("leveltop")) {
            plugin.getGuiManager().openTop(player);
            return true;
        }
        if (command.getName().equalsIgnoreCase("levelgift")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("get")) {
                int amount = plugin.getGiftManager().claimAll(player, data);
                if (amount <= 0) {
                    player.sendMessage(ColorUtil.component(plugin.msg("gift-none")));
                } else {
                    player.sendMessage(ColorUtil.component(plugin.msg("gift-claimed").replace("%count%", String.valueOf(amount))));
                }
                return true;
            }
            plugin.getGuiManager().openGift(player, 1);
            return true;
        }
        return false;
    }
}
