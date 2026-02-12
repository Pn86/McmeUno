package com.pn86.pnvip;

import com.pn86.pnvip.command.PnVipCommand;
import com.pn86.pnvip.command.VipCommand;
import com.pn86.pnvip.command.VipSigninCommand;
import com.pn86.pnvip.data.DataStore;
import com.pn86.pnvip.model.PlayerVipRecord;
import com.pn86.pnvip.model.VipDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import com.pn86.pnvip.papi.PnVipPlaceholderExpansion;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PnVipPlugin extends JavaPlugin {
    private DataStore dataStore;
    private VipManager vipManager;
    private BukkitTask expireTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfNotExists("vip.yml");
        saveResourceIfNotExists("data.yml");

        this.dataStore = new DataStore(this);
        this.vipManager = new VipManager(this, dataStore);

        vipManager.reloadAll();

        getCommand("vip").setExecutor(new VipCommand(this));
        getCommand("vipsignin").setExecutor(new VipSigninCommand(this));
        getCommand("pnvip").setExecutor(new PnVipCommand(this));

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new VipListener(this), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            vipManager.applyPermissions(player);
        }

        startExpireTask();
        registerPlaceholders();
        getLogger().info("PnVIP enabled.");
    }

    @Override
    public void onDisable() {
        if (expireTask != null) {
            expireTask.cancel();
            expireTask = null;
        }
        if (vipManager != null) {
            vipManager.clearAttachments();
        }
        if (dataStore != null) {
            dataStore.save();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        vipManager.reloadAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            vipManager.applyPermissions(player);
        }
    }

    private void startExpireTask() {
        expireTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            boolean changed = false;
            for (UUID uuid : new HashSet<>(dataStore.getAllPlayers())) {
                PlayerVipRecord record = dataStore.getRecord(uuid);
                if (record == null) {
                    continue;
                }
                if (vipManager.removeExpired(record)) {
                    changed = true;
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        vipManager.applyPermissions(player);
                    }
                }
            }
            if (changed) {
                dataStore.save();
            }
        }, 20L, 20L * 60L);
    }


    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PnVipPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI hook enabled.");
        }
    }

    private void saveResourceIfNotExists(String name) {
        if (!new java.io.File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public VipManager getVipManager() {
        return vipManager;
    }

    public String msg(String path) {
        return vipManager.color(getConfig().getString("messages." + path, ""));
    }

    public List<String> msgList(String path) {
        List<String> list = getConfig().getStringList("messages." + path);
        List<String> out = new ArrayList<>();
        for (String s : list) {
            out.add(vipManager.color(s));
        }
        return out;
    }

    public Collection<VipDefinition> getVipDefinitions() {
        return vipManager.getDefinitions();
    }
}
