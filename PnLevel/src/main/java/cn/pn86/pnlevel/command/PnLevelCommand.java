package cn.pn86.pnlevel.command;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PnLevelCommand implements CommandExecutor, TabCompleter {
    private final PnLevelPlugin plugin;

    public PnLevelCommand(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.component("&e/pnlv reload|list|look|set|add|remove|reset"));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(ColorUtil.component(plugin.msg("reload")));
            }
            case "list" -> {
                int page = args.length >= 2 ? Math.max(1, parseInt(args[1], 1)) : 1;
                List<PlayerLevelData> all = new ArrayList<>(plugin.getPlayerDataManager().all());
                all.sort(Comparator.comparing(PlayerLevelData::getLastName, String.CASE_INSENSITIVE_ORDER));
                int start = (page - 1) * 20;
                int end = Math.min(start + 20, all.size());
                sender.sendMessage(ColorUtil.component("&6--- PnLevel 玩家列表 第" + page + "页 ---"));
                for (int i = start; i < end; i++) {
                    PlayerLevelData data = all.get(i);
                    sender.sendMessage(ColorUtil.component("&f" + data.getLastName() + " &7- Lv." + data.getLevel() + " EXP:" + data.getExp()));
                }
            }
            case "look" -> {
                if (args.length < 2) return false;
                PlayerLevelData data = plugin.findPlayerData(args[1]);
                if (data == null) {
                    sender.sendMessage(ColorUtil.component(plugin.msg("player-not-found")));
                    return true;
                }
                sender.sendMessage(ColorUtil.component("&e" + data.getLastName() + " &f等级: &6" + data.getLevel() + " &f经验: &b" + data.getExp()));
            }
            case "set", "add", "remove" -> {
                if (args.length < 4) return false;
                PlayerLevelData data = plugin.findPlayerData(args[1]);
                if (data == null) {
                    sender.sendMessage(ColorUtil.component(plugin.msg("player-not-found")));
                    return true;
                }
                String type = args[2].toLowerCase(Locale.ROOT);
                int value = parseInt(args[3], 0);
                if (type.equals("exp")) value = Math.min(999, Math.max(0, value));
                if (args[0].equalsIgnoreCase("set")) {
                    if (type.equals("exp")) data.setExp(value); else data.setLevel(Math.min(plugin.getMaxLevel(), value));
                } else if (args[0].equalsIgnoreCase("add")) {
                    if (type.equals("exp")) data.setExp(Math.min(999, data.getExp() + value)); else data.setLevel(Math.min(plugin.getMaxLevel(), data.getLevel() + value));
                } else {
                    if (type.equals("exp")) data.setExp(Math.max(0, data.getExp() - value)); else data.setLevel(Math.max(1, data.getLevel() - value));
                }
                sender.sendMessage(ColorUtil.component(plugin.msg("admin-updated")));
            }
            case "reset" -> {
                if (args.length < 2) return false;
                PlayerLevelData data = plugin.findPlayerData(args[1]);
                if (data == null) {
                    sender.sendMessage(ColorUtil.component(plugin.msg("player-not-found")));
                    return true;
                }
                data.setLevel(plugin.getInitialLevel());
                data.setExp(plugin.getInitialExp());
                data.setLastClaimedLevel(0);
                sender.sendMessage(ColorUtil.component(plugin.msg("admin-reset")));
            }
            default -> sender.sendMessage(ColorUtil.component("&c未知子命令"));
        }
        return true;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("reload", "list", "look", "set", "add", "remove", "reset");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("list")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return Arrays.asList("exp", "level");
        }
        return Collections.emptyList();
    }
}
