package cn.pn86.pnwarp;

import cn.pn86.pnwarp.command.WarpCommand;
import cn.pn86.pnwarp.gui.WarpGuiListener;
import cn.pn86.pnwarp.gui.WarpGuiManager;
import cn.pn86.pnwarp.placeholder.PnWarpPlaceholderExpansion;
import cn.pn86.pnwarp.service.WarpService;
import cn.pn86.pnwarp.service.WarpStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PnWarpPlugin extends JavaPlugin {
    private WarpStorage storage;
    private WarpService warpService;
    private WarpGuiManager guiManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("gui.yml", false);
        saveResource("data.yml", false);

        storage = new WarpStorage(this);
        storage.load();

        warpService = new WarpService(this, storage);
        warpService.loadDefaultsIfNeeded();

        guiManager = new WarpGuiManager(this, warpService);

        WarpCommand warpCommand = new WarpCommand(this, warpService, guiManager);
        registerCommand("addwarp", warpCommand);
        registerCommand("remwarp", warpCommand);
        registerCommand("warps", warpCommand);
        registerCommand("gowarp", warpCommand);
        registerCommand("pnwp", warpCommand);

        getServer().getPluginManager().registerEvents(new WarpGuiListener(guiManager, warpService), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PnWarpPlaceholderExpansion(warpService).register();
            getLogger().info("PlaceholderAPI detected, placeholders enabled.");
        }
    }

    @Override
    public void onDisable() {
        if (warpService != null) {
            warpService.shutdown();
        }
    }

    public void reloadEverything() {
        reloadConfig();
        storage.load();
        warpService.loadDefaultsIfNeeded();
        guiManager.reload();
    }

    private void registerCommand(String name, WarpCommand command) {
        PluginCommand pluginCommand = getCommand(name);
        if (pluginCommand == null) {
            getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }
}
