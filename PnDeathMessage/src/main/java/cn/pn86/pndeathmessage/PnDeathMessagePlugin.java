package cn.pn86.pndeathmessage;

import cn.pn86.pndeathmessage.command.PnDeathMessageCommand;
import cn.pn86.pndeathmessage.config.NameConfigManager;
import cn.pn86.pndeathmessage.listener.DeathMessageListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PnDeathMessagePlugin extends JavaPlugin {

    private NameConfigManager nameConfigManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("attack.yml");
        saveResourceIfMissing("item.yml");

        this.nameConfigManager = new NameConfigManager(this);
        this.nameConfigManager.reload();

        getServer().getPluginManager().registerEvents(new DeathMessageListener(this), this);

        PluginCommand command = getCommand("pndm");
        if (command != null) {
            command.setExecutor(new PnDeathMessageCommand(this));
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        this.nameConfigManager.reload();
    }

    public NameConfigManager getNameConfigManager() {
        return nameConfigManager;
    }

    private void saveResourceIfMissing(String resource) {
        if (getResource(resource) != null) {
            saveResource(resource, false);
        }
    }
}
