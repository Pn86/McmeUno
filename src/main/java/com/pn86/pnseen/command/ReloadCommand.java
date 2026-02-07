package com.pn86.pnseen.command;

import com.pn86.pnseen.PnSeenPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final PnSeenPlugin plugin;

    public ReloadCommand(PnSeenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnseen.use")) {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.prefix")
                    + plugin.getMessage("messages.no-permission")));
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.prefix")
                    + plugin.getMessage("messages.usage-reload")));
            return true;
        }
        plugin.reloadConfig();
        sender.sendMessage(plugin.colorize(plugin.getMessage("messages.prefix")
                + plugin.getMessage("messages.reload-success")));
        return true;
    }
}
