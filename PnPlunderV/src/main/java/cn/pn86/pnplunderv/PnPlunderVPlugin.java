package cn.pn86.pnplunderv;

import cn.pn86.pnplunderv.command.PnPlunderCommand;
import cn.pn86.pnplunderv.listener.PvpPlunderListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PnPlunderVPlugin extends JavaPlugin {

    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy provider not found, plugin disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PluginCommand command = getCommand("pnpv");
        if (command == null) {
            getLogger().severe("Command pnpv is missing in plugin.yml, plugin disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(new PnPlunderCommand(this));
        Bukkit.getPluginManager().registerEvents(new PvpPlunderListener(this), this);
        getLogger().info("PnPlunderV enabled.");
    }

    public Economy getEconomy() {
        return economy;
    }

    public void reloadPluginConfig() {
        reloadConfig();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
}
