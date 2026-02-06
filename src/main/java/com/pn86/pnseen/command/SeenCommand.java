package com.pn86.pnseen.command;

import com.pn86.pnseen.PnSeenPlugin;
import com.pn86.pnseen.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SeenCommand implements CommandExecutor {
    private final PnSeenPlugin plugin;

    public SeenCommand(PnSeenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pnseen.use")) {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.prefix")
                    + plugin.getMessage("messages.no-permission")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.prefix")
                    + plugin.getMessage("messages.usage-seen")));
            return true;
        }

        String target = args[0];
        Player player = Bukkit.getPlayerExact(target);
        PlayerData data = null;

        if (player != null) {
            data = plugin.getDataStore().getOrCreate(player.getUniqueId());
        } else {
            try {
                UUID uuid = UUID.fromString(target);
                data = plugin.getDataStore().get(uuid);
                if (data == null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if (offlinePlayer.getName() != null) {
                        data = plugin.getDataStore().getOrCreate(uuid);
                        data.setName(offlinePlayer.getName());
                    }
                }
            } catch (IllegalArgumentException ignored) {
                data = plugin.getDataStore().findByName(target);
                if (data == null) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);
                    if (offlinePlayer != null && offlinePlayer.getUniqueId() != null) {
                        data = plugin.getDataStore().get(offlinePlayer.getUniqueId());
                    }
                }
            }
        }

        if (data == null || data.getName() == null) {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.prefix")
                    + plugin.getMessage("messages.player-not-found")));
            return true;
        }

        boolean isOnline = player != null;
        long now = System.currentTimeMillis();
        long totalPlaytime = data.getTotalPlaytimeMillis();
        long sessionTime = 0;
        if (isOnline && data.getLastJoinMillis() > 0) {
            sessionTime = now - data.getLastJoinMillis();
        }
        String playtimeText = formatDuration(totalPlaytime + sessionTime);

        String offlineText = "";
        if (!isOnline) {
            if (data.getLastSeenMillis() > 0) {
                offlineText = formatDuration(now - data.getLastSeenMillis());
            } else {
                offlineText = "未知";
            }
        }

        Location location = null;
        String worldName = data.getLastWorld();
        double x = data.getLastX();
        double y = data.getLastY();
        double z = data.getLastZ();

        if (isOnline) {
            location = player.getLocation();
            worldName = location.getWorld() != null ? location.getWorld().getName() : worldName;
            x = location.getX();
            y = location.getY();
            z = location.getZ();
        }

        sendInfo(sender, data, isOnline, playtimeText, offlineText, worldName, x, y, z);
        return true;
    }

    private void sendInfo(CommandSender sender, PlayerData data, boolean isOnline,
                          String playtimeText, String offlineText,
                          String worldName, double x, double y, double z) {
        sender.sendMessage(plugin.colorize(replace(plugin.getMessage("messages.info.name"), data)));
        sender.sendMessage(plugin.colorize(replace(plugin.getMessage("messages.info.uuid"), data)));
        sender.sendMessage(plugin.colorize(plugin.getMessage("messages.info.playtime")
                .replace("%playtime%", playtimeText)));

        if (isOnline) {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.info.online")
                    .replace("%online%", playtimeText)));
        } else {
            sender.sendMessage(plugin.colorize(plugin.getMessage("messages.info.offline")
                    .replace("%offline%", offlineText)
                    .replace("%online%", playtimeText)));
        }

        sender.sendMessage(plugin.colorize(plugin.getMessage("messages.info.location")
                .replace("%x%", formatLocation(x))
                .replace("%y%", formatLocation(y))
                .replace("%z%", formatLocation(z))
                .replace("%world%", worldName != null ? worldName : "未知")));

        String ip = data.getLastIp() != null ? data.getLastIp() : "未知";
        sender.sendMessage(plugin.colorize(plugin.getMessage("messages.info.ip")
                .replace("%ip%", ip)));
    }

    private String replace(String message, PlayerData data) {
        return message.replace("%name%", data.getName() != null ? data.getName() : "未知")
                .replace("%uuid%", data.getUuid().toString());
    }

    private String formatLocation(double value) {
        return String.format("%.2f", value);
    }

    private String formatDuration(long millis) {
        long minutes = millis / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            long remainingHours = hours % 24;
            long remainingMinutes = minutes % 60;
            return days + "天" + remainingHours + "小时" + remainingMinutes + "分钟";
        }
        if (hours > 0) {
            long remainingMinutes = minutes % 60;
            return hours + "小时" + remainingMinutes + "分钟";
        }
        return minutes + "分钟";
    }
}
