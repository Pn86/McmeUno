package com.pn86.pnvip.command;

import com.pn86.pnvip.PnVipPlugin;
import com.pn86.pnvip.model.PlayerVipRecord;
import com.pn86.pnvip.model.VipDefinition;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class VipCommand implements CommandExecutor {
    private final PnVipPlugin plugin;

    public VipCommand(PnVipPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        PlayerVipRecord record = plugin.getDataStore().getRecord(player.getUniqueId());
        if (record == null || record.getVipExpireAt().isEmpty()) {
            player.sendMessage(plugin.msg("vip-empty"));
            return true;
        }

        player.sendMessage(plugin.msg("vip-header"));
        for (Map.Entry<String, Long> entry : record.getVipExpireAt().entrySet()) {
            VipDefinition definition = plugin.getVipManager().getDefinition(entry.getKey());
            String display = definition == null ? entry.getKey() : definition.displayName();
            String line = plugin.msg("vip-line")
                    .replace("%vip%", plugin.getVipManager().color(display))
                    .replace("%expire%", plugin.getVipManager().formatExpireAt(entry.getValue()));
            player.sendMessage(line);
        }
        return true;
    }
}
