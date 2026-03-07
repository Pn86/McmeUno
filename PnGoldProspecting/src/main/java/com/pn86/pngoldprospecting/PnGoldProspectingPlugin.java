package com.pn86.pngoldprospecting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PnGoldProspectingPlugin extends JavaPlugin {
    private DataManager dataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.dataManager = new DataManager(this);
        dataManager.loadAll(false);

        PnGpCommand command = new PnGpCommand(this, dataManager);
        getCommand("pngp").setExecutor(command);
        getCommand("pngp").setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(new ProspectingListener(this, dataManager), this);
        startResetTask();
        getLogger().info("PnGoldProspecting 已启用");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        dataManager.loadAll(true);
    }

    private void startResetTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (ProspectingBlock block : dataManager.getBlocks().values()) {
                boolean wasOpened = block.isOpened();
                block.tickReset();
                if (wasOpened && !block.isOpened()) {
                    dataManager.applyCurrentAppearance(block);
                    dataManager.saveBlock(block);
                }
            }
        }, 20L, 20L);
    }

    public Component msg(String key) {
        FileConfiguration cfg = getConfig();
        String raw = cfg.getString("messages." + key, key);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }
}
