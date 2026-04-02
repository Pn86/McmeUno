package uno.mcme.pnoremine.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.MineRegion;

import java.util.stream.Collectors;

public class PnOreMineCommand implements CommandExecutor {

    private final PnOreMinePlugin plugin;

    public PnOreMineCommand(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnoremine.admin")) {
            sender.sendMessage(plugin.getPrefix() + plugin.msg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                boolean ok = plugin.reloadMinePlugin();
                if (ok) {
                    sender.sendMessage(plugin.getPrefix() + plugin.msg("reloaded"));
                }
                return true;
            }
            case "list" -> {
                String names = plugin.getMineManager().getMines().stream().map(MineRegion::getName).collect(Collectors.joining(", "));
                sender.sendMessage(plugin.getPrefix() + plugin.msg("mine-list-title").replace("%mines%", names));
                return true;
            }
            case "see" -> {
                if (args.length < 2) {
                    return false;
                }
                MineRegion mine = plugin.getMineManager().findMine(args[1]);
                if (mine == null) {
                    sender.sendMessage(plugin.getPrefix() + plugin.msg("mine-not-found").replace("%mine%", args[1]));
                    return true;
                }
                sender.sendMessage(plugin.getPrefix() + plugin.msg("mine-see")
                    .replace("%mine%", mine.getName())
                    .replace("%world%", mine.getWorldName())
                    .replace("%time%", String.valueOf(mine.getRemainingSeconds())));
                return true;
            }
            case "reset" -> {
                if (args.length < 2) {
                    return false;
                }
                MineRegion mine = plugin.getMineManager().findMine(args[1]);
                if (mine == null) {
                    sender.sendMessage(plugin.getPrefix() + plugin.msg("mine-not-found").replace("%mine%", args[1]));
                    return true;
                }
                mine.reset();
                sender.sendMessage(plugin.getPrefix() + plugin.msg("mine-reset").replace("%mine%", mine.getName()));
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
