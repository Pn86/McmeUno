package cn.pn86.pnbooknews.command;

import cn.pn86.pnbooknews.PnBookNewsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PnBookNewsCommand implements CommandExecutor {

    private final PnBookNewsPlugin plugin;

    public PnBookNewsCommand(PnBookNewsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnbooknews.admin")) {
            sender.sendMessage(plugin.getPrefix() + plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            boolean ok = plugin.reloadBookNews();
            sender.sendMessage(plugin.getPrefix() + (ok ? plugin.msg("reloaded") : plugin.msg("reload-failed")));
            return true;
        }

        return false;
    }
}
