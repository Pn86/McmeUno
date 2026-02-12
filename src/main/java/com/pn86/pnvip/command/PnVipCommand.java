package com.pn86.pnvip.command;

import com.pn86.pnvip.PnVipPlugin;
import com.pn86.pnvip.model.VipDefinition;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public class PnVipCommand implements CommandExecutor {
    private final PnVipPlugin plugin;

    public PnVipCommand(PnVipPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.msgList("admin-usage").forEach(sender::sendMessage);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(plugin.msg("reload-success"));
            }
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "clear" -> handleClear(sender, args);
            default -> plugin.msgList("admin-usage").forEach(sender::sendMessage);
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(plugin.msg("add-usage"));
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        if (player.getUniqueId() == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }

        VipDefinition definition = plugin.getVipManager().getDefinition(args[2]);
        if (definition == null) {
            sender.sendMessage(plugin.msg("vip-not-found"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.msg("time-invalid"));
            return;
        }

        long seconds = plugin.getVipManager().parseDurationSeconds(amount, args[4]);
        if (seconds <= 0) {
            sender.sendMessage(plugin.msg("time-invalid")
                    .replace("%units%", String.join(",", plugin.getVipManager().supportedUnits())));
            return;
        }

        long expireAt = plugin.getVipManager().grantVip(player.getUniqueId(), player.getName() == null ? args[1] : player.getName(), definition.key(), seconds);
        sender.sendMessage(plugin.msg("add-success")
                .replace("%player%", args[1])
                .replace("%vip%", plugin.getVipManager().color(definition.displayName()))
                .replace("%expire%", plugin.getVipManager().formatExpireAt(expireAt)));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("remove-usage"));
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        if (player.getUniqueId() == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }
        boolean ok = plugin.getVipManager().removeVip(player.getUniqueId(), args[2]);
        if (ok) {
            sender.sendMessage(plugin.msg("remove-success")
                    .replace("%player%", args[1])
                    .replace("%vip%", args[2]));
        } else {
            sender.sendMessage(plugin.msg("remove-fail"));
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("clear-usage"));
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
        if (player.getUniqueId() == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }
        boolean ok = plugin.getVipManager().clearVip(player.getUniqueId());
        if (ok) {
            sender.sendMessage(plugin.msg("clear-success").replace("%player%", args[1]));
        } else {
            sender.sendMessage(plugin.msg("clear-fail"));
        }
    }
}
