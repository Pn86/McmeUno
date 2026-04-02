package uno.mcme.pnoremine.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.MineRegion;

import java.util.Map;
import java.util.stream.Collectors;

public class PnOreMineCommand implements CommandExecutor {

    private final PnOreMinePlugin plugin;

    public PnOreMineCommand(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnoremine.admin")) {
            plugin.sendLocalized(sender, "no-permission", Map.of(), "[message]&c你没有权限执行这个命令。");
            return true;
        }
        if (args.length == 0) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                boolean ok = plugin.reloadMinePlugin();
                if (ok) {
                    plugin.sendLocalized(sender, "reloaded", Map.of(), "[message]&a插件已重载。");
                }
                return true;
            }
            case "list" -> {
                String names = plugin.getMineManager().getMines().stream().map(MineRegion::getName).collect(Collectors.joining(", "));
                plugin.sendLocalized(sender, "mine-list-title", Map.of("mines", names), "[message]&e矿场列表: %mines%");
                return true;
            }
            case "see" -> {
                if (args.length < 2) {
                    return false;
                }
                MineRegion mine = plugin.getMineManager().findMine(args[1]);
                if (mine == null) {
                    plugin.sendLocalized(sender, "mine-not-found", Map.of("mine", args[1]), "[message]&c找不到矿场: %mine%");
                    return true;
                }
                plugin.sendLocalized(sender, "mine-see", Map.of(
                    "mine", mine.getName(),
                    "world", mine.getWorldName(),
                    "time", String.valueOf(mine.getRemainingSeconds())
                ), "[message]&e矿场 &6%mine% &e世界: &f%world% &e剩余: &f%time%s");
                return true;
            }
            case "reset" -> {
                if (args.length < 2) {
                    return false;
                }
                MineRegion mine = plugin.getMineManager().findMine(args[1]);
                if (mine == null) {
                    plugin.sendLocalized(sender, "mine-not-found", Map.of("mine", args[1]), "[message]&c找不到矿场: %mine%");
                    return true;
                }
                plugin.resetMineWithSafety(mine, true);
                plugin.sendLocalized(sender, "mine-reset", Map.of("mine", mine.getName()), "[message]&a矿场 %mine% 已刷新。");
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
