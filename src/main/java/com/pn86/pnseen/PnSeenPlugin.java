package com.pn86.pnseen;

import com.pn86.pnseen.command.ReloadCommand;
import com.pn86.pnseen.command.SeenCommand;
import com.pn86.pnseen.data.PlayerData;
import com.pn86.pnseen.data.PlayerDataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;
import java.util.UUID;

public class PnSeenPlugin extends JavaPlugin implements Listener {
    private PlayerDataStore dataStore;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataStore = new PlayerDataStore(this);
        dataStore.load();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand seenCommand = getCommand("seen");
        if (seenCommand != null) {
            seenCommand.setExecutor(new SeenCommand(this));
        }
        PluginCommand reloadCommand = getCommand("pnseen");
        if (reloadCommand != null) {
            reloadCommand.setExecutor(new ReloadCommand(this));
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerData(player, now, true);
            }
            dataStore.save();
        }, 20L * 60, 20L * 60);
    }

    @Override
    public void onDisable() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerData(player, now, false);
        }
        dataStore.save();
    }

    public PlayerDataStore getDataStore() {
        return dataStore;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = dataStore.getOrCreate(player.getUniqueId());
        data.setName(player.getName());
        data.setLastJoinMillis(System.currentTimeMillis());
        updateLocationAndIp(player, data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        PlayerData data = dataStore.getOrCreate(player.getUniqueId());
        data.setName(player.getName());
        if (data.getLastJoinMillis() > 0) {
            data.setTotalPlaytimeMillis(data.getTotalPlaytimeMillis() + (now - data.getLastJoinMillis()));
        }
        data.setLastJoinMillis(0);
        data.setLastSeenMillis(now);
        updateLocationAndIp(player, data);
        dataStore.save();
    }

    private void updatePlayerData(Player player, long now, boolean rollingUpdate) {
        PlayerData data = dataStore.getOrCreate(player.getUniqueId());
        data.setName(player.getName());
        if (data.getLastJoinMillis() > 0) {
            long delta = now - data.getLastJoinMillis();
            data.setTotalPlaytimeMillis(data.getTotalPlaytimeMillis() + delta);
            if (rollingUpdate) {
                data.setLastJoinMillis(now);
            }
        } else if (rollingUpdate) {
            data.setLastJoinMillis(now);
        }
        updateLocationAndIp(player, data);
    }

    private void updateLocationAndIp(Player player, PlayerData data) {
        Location location = player.getLocation();
        data.setLastWorld(location.getWorld() != null ? location.getWorld().getName() : null);
        data.setLastX(location.getX());
        data.setLastY(location.getY());
        data.setLastZ(location.getZ());
        InetSocketAddress address = player.getAddress();
        if (address != null && address.getAddress() != null) {
            data.setLastIp(address.getAddress().getHostAddress());
        }
    }

    public String getMessage(String path) {
        return getConfig().getString(path, "");
    }

    public String colorize(String message) {
        return message.replace("&", "§");
    }

    public PlayerData getData(UUID uuid) {
        return dataStore.get(uuid);
    }
}
