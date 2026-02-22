package cn.pn86.pnextremesurvival.command;

import cn.pn86.pnextremesurvival.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinLeaveCommand implements CommandExecutor {

    private final GameManager gameManager;

    public JoinLeaveCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (command.getName().equalsIgnoreCase("leave")) {
            gameManager.setOptOut(player, true);
        } else if (command.getName().equalsIgnoreCase("join")) {
            gameManager.setOptOut(player, false);
        }
        return true;
    }
}
