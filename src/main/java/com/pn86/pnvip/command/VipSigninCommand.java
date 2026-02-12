package com.pn86.pnvip.command;

import com.pn86.pnvip.PnVipPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VipSigninCommand implements CommandExecutor {
    private final PnVipPlugin plugin;

    public VipSigninCommand(PnVipPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }

        boolean ok = plugin.getVipManager().signin(player);
        if (ok) {
            player.sendMessage(plugin.msg("signin-success"));
        } else {
            player.sendMessage(plugin.msg("signin-none"));
        }
        return true;
    }
}
