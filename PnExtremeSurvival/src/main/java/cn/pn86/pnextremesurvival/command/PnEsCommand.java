package cn.pn86.pnextremesurvival.command;

import cn.pn86.pnextremesurvival.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PnEsCommand implements CommandExecutor {

    private final GameManager gameManager;

    public PnEsCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnes.admin")) {
            sender.sendMessage("§c没有权限。");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/pnes start|stop|resetworld|reload|mode [solo/multiplayer]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "start" -> gameManager.forceStart();
            case "stop" -> gameManager.forceStop();
            case "resetworld" -> gameManager.resetWorldAndShutdown();
            case "reload" -> {
                gameManager.reload();
                sender.sendMessage("§a已重载配置。");
            }
            case "mode" -> {
                if (args.length < 2 || (!args[1].equalsIgnoreCase("solo") && !args[1].equalsIgnoreCase("multiplayer"))) {
                    sender.sendMessage("§c用法: /pnes mode [solo/multiplayer]");
                } else {
                    gameManager.setMode(args[1].toLowerCase());
                    sender.sendMessage("§a已切换为 " + args[1] + " 模式。");
                }
            }
            default -> sender.sendMessage("§c未知子命令。");
        }
        return true;
    }
}
