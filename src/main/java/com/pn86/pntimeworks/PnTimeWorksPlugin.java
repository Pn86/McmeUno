package com.pn86.pntimeworks;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class PnTimeWorksPlugin extends JavaPlugin {
    private final WorkManager workManager = new WorkManager(this);
    private BukkitTask schedulerTask;
    private File worksFile;
    private FileConfiguration worksConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadWorksFile();

        workManager.loadFromConfig(worksConfig);
        registerCommands();
        startScheduler();

        getLogger().info("PnTimeWorks enabled. Loaded work groups: " + workManager.getWorkGroups().size());
    }

    @Override
    public void onDisable() {
        stopScheduler();
    }

    public void reloadAll() {
        reloadConfig();
        loadWorksFile();
        workManager.loadFromConfig(worksConfig);
    }

    private void registerCommands() {
        PnTimeWorksCommand commandExecutor = new PnTimeWorksCommand(this);
        if (getCommand("pntws") != null) {
            getCommand("pntws").setExecutor(commandExecutor);
            getCommand("pntws").setTabCompleter(commandExecutor);
        } else {
            getLogger().warning("Command pntws is missing in plugin.yml");
        }
    }

    private void startScheduler() {
        stopScheduler();
        schedulerTask = getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                workManager.tick();
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Error while running scheduled works", ex);
            }
        }, 20L, 20L);
    }

    private void stopScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }

    private void loadWorksFile() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder");
        }

        worksFile = new File(getDataFolder(), "works.yml");
        if (!worksFile.exists()) {
            saveResource("works.yml", false);
        }

        worksConfig = YamlConfiguration.loadConfiguration(worksFile);
    }

    public void saveWorksConfig() {
        try {
            worksConfig.save(worksFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save works.yml", e);
        }
    }

    public String message(String path) {
        return applyPrefix(color(getConfig().getString("messages." + path, "")));
    }

    public String message(String path, Map<String, String> placeholders) {
        String msg = getConfig().getString("messages." + path, "");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return applyPrefix(color(msg));
    }

    private String applyPrefix(String message) {
        String prefix = color(getConfig().getString("messages.prefix", ""));
        if (message == null || message.isBlank()) {
            return "";
        }
        if ("prefix".equalsIgnoreCase(message)) {
            return prefix;
        }
        return prefix + message;
    }

    private String color(String input) {
        return input.replace('&', '§');
    }

    public List<WorkGroup> getWorkGroups() {
        return Collections.unmodifiableList(workManager.getWorkGroups());
    }
}
