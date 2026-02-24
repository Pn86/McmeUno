package cn.pn86.pndeathmessage.command;

import cn.pn86.pndeathmessage.PnDeathMessagePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PnDeathMessageCommand implements CommandExecutor {

    private final PnDeathMessagePlugin plugin;

    public PnDeathMessageCommand(PnDeathMessagePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pndeathmessage.reload")) {
            sender.sendMessage(color("&c你没有权限使用该指令。"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(color("&a[PnDeathMessage] 配置已重载。"));
            return true;
        }

        sender.sendMessage(color("&e用法: /pndm reload"));
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
