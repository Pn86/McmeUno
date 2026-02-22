package cn.pn86.pnextremesurvival;

import cn.pn86.pnextremesurvival.command.JoinLeaveCommand;
import cn.pn86.pnextremesurvival.command.PnEsCommand;
import cn.pn86.pnextremesurvival.config.MessageManager;
import cn.pn86.pnextremesurvival.game.GameListener;
import cn.pn86.pnextremesurvival.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class PnExtremeSurvivalPlugin extends JavaPlugin {

    private MessageManager messageManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("message.yml", false);

        this.messageManager = new MessageManager(this);
        this.gameManager = new GameManager(this, messageManager);

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);

        JoinLeaveCommand joinLeaveCommand = new JoinLeaveCommand(gameManager);
        getCommand("join").setExecutor(joinLeaveCommand);
        getCommand("leave").setExecutor(joinLeaveCommand);
        getCommand("pnes").setExecutor(new PnEsCommand(gameManager));

        gameManager.startLoops();
        Bukkit.getLogger().info("[PnExtremeSurvival] Enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
    }

    public void reloadAll() {
        reloadConfig();
        messageManager.reload();
        gameManager.reload();
    }
}
