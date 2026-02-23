package uno.mcme.pnspeedlimit;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import uno.mcme.pnspeedlimit.command.PnslCommand;
import uno.mcme.pnspeedlimit.listener.SpeedLimitListener;

public class PnSpeedLimitPlugin extends JavaPlugin {

    private SpeedLimitManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        manager = new SpeedLimitManager(this);
        manager.reload();

        PnslCommand command = new PnslCommand(this, manager);
        PluginCommand pnsl = getCommand("pnsl");
        if (pnsl != null) {
            pnsl.setExecutor(command);
            pnsl.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new SpeedLimitListener(manager), this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiExpansion(this, manager).register();
            getLogger().info("PlaceholderAPI detected, placeholders registered.");
        }
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.save();
        }
    }
}
