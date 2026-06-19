package uno.mcme.pnplayertask.command;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import uno.mcme.pnplayertask.PnPlayerTaskPlugin;

import java.util.List;

public class TaskCommands implements CommandExecutor, TabCompleter {
    private final PnPlayerTaskPlugin plugin;
    public TaskCommands(PnPlayerTaskPlugin plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("task")) {
            if (!(sender instanceof Player p)) { plugin.msg(sender, "player-only"); return true; }
            if (!p.hasPermission("pnplayertask.use")) { plugin.msg(sender, "no-permission"); return true; }
            plugin.getTaskGui().open(p, 0); return true;
        }
        if (!sender.hasPermission("pnplayertask.admin")) { plugin.msg(sender, "no-permission"); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) { plugin.reloadAll(); plugin.msg(sender, "reload"); return true; }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) { boolean ok = plugin.getTaskManager().reset(args[1]); plugin.msg(sender, ok ? "reset-success" : "player-not-found", "%player%", args[1]); return true; }
        plugin.msg(sender, "help"); return true;
    }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) { if (!c.getName().equalsIgnoreCase("pnplayertask")) return List.of(); if (args.length == 1) return List.of("reload", "reset"); if (args.length == 2 && args[0].equalsIgnoreCase("reset")) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(); return List.of(); }
}
