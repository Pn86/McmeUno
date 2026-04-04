package cn.pn86.pnplunderv.command;

import cn.pn86.pnplunderv.PnPlunderVPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PnPlunderCommand implements CommandExecutor {

    private final PnPlunderVPlugin plugin;

    public PnPlunderCommand(PnPlunderVPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnpv.admin")) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.no-permission", "&c你没有权限执行此命令")));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPluginConfig();
            sender.sendMessage(color(plugin.getConfig().getString("messages.reload-success", "&aPnPlunderV 配置重载完成")));
            return true;
        }

        sender.sendMessage(color(plugin.getConfig().getString("messages.usage", "&e用法: /pnpv reload")));
        return true;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
