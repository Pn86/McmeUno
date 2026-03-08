package cn.pn86.pnwarp.command;

import cn.pn86.pnwarp.PnWarpPlugin;
import cn.pn86.pnwarp.gui.WarpGuiManager;
import cn.pn86.pnwarp.model.Warp;
import cn.pn86.pnwarp.service.WarpService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import cn.pn86.pnwarp.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class WarpCommand implements CommandExecutor, TabCompleter {
    private final PnWarpPlugin plugin;
    private final WarpService warpService;
    private final WarpGuiManager guiManager;

    public WarpCommand(PnWarpPlugin plugin, WarpService warpService, WarpGuiManager guiManager) {
        this.plugin = plugin;
        this.warpService = warpService;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "addwarp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(warpService.msg("messages.players-only"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(warpService.msg("messages.usage-addwarp"));
                    return true;
                }
                String warpName = args[0];
                String icon = args[args.length - 1];
                String description = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
                Optional<String> error = warpService.addPlayerWarp(player, warpName, description, icon);
                if (error.isPresent()) {
                    player.sendMessage(error.get());
                    return true;
                }
                player.sendMessage(warpService.msg("messages.add-success").replace("{warp}", TextUtil.color(warpName)));
                return true;
            }
            case "remwarp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(warpService.msg("messages.players-only"));
                    return true;
                }
                if (args.length != 1) {
                    sender.sendMessage(warpService.msg("messages.usage-remwarp"));
                    return true;
                }
                boolean success = warpService.removeWarpOwnedBy(player, args[0]);
                player.sendMessage(success
                        ? warpService.msg("messages.rem-success").replace("{warp}", TextUtil.color(args[0]))
                        : warpService.msg("messages.rem-fail"));
                return true;
            }
            case "warps" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(warpService.msg("messages.players-only"));
                    return true;
                }
                guiManager.open(player, 1);
                return true;
            }
            case "gowarp" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(warpService.msg("messages.players-only"));
                    return true;
                }
                if (args.length != 1) {
                    sender.sendMessage(warpService.msg("messages.usage-gowarp"));
                    return true;
                }
                Optional<Warp> warp = warpService.getWarp(args[0]);
                if (warp.isEmpty()) {
                    player.sendMessage(warpService.msg("messages.warp-not-found"));
                    return true;
                }
                warpService.scheduleTeleport(player, warp.get());
                return true;
            }
            case "pnwp" -> {
                return handleAdmin(sender, args);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(warpService.msg("messages.usage-pnwp"));
            return true;
        }
        if (!sender.hasPermission("pnwarp.admin")) {
            sender.sendMessage(warpService.msg("messages.no-permission"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadEverything();
                sender.sendMessage(warpService.msg("messages.reload"));
            }
            case "remwarp" -> {
                if (args.length != 2) {
                    sender.sendMessage(warpService.msg("messages.usage-pnwp-remwarp"));
                    return true;
                }
                boolean success = warpService.removeWarpAny(args[1]);
                sender.sendMessage(success
                        ? warpService.msg("messages.rem-success").replace("{warp}", TextUtil.color(args[1]))
                        : warpService.msg("messages.warp-not-found"));
            }
            case "remove" -> {
                if (args.length != 2) {
                    sender.sendMessage(warpService.msg("messages.usage-pnwp-remove"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                int removed = warpService.removePlayerWarps(target.getUniqueId());
                sender.sendMessage(warpService.msg("messages.remove-player-success")
                        .replace("{player}", args[1])
                        .replace("{count}", String.valueOf(removed)));
            }
            case "removeall" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
                    sender.sendMessage(warpService.msg("messages.removeall-confirm"));
                    return true;
                }
                int removed = warpService.removeAllWarps();
                sender.sendMessage(warpService.msg("messages.removeall-success").replace("{count}", String.valueOf(removed)));
            }
            default -> sender.sendMessage(warpService.msg("messages.usage-pnwp"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (cmd.equals("pnwp")) {
            if (args.length == 1) {
                return filter(List.of("reload", "remwarp", "remove", "removeall"), args[0]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remwarp")) {
                for (Warp warp : warpService.allWarpsSorted()) {
                    out.add(warp.name());
                }
                return filter(out, args[1]);
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("removeall")) {
                return filter(List.of("confirm"), args[1]);
            }
        }

        if ((cmd.equals("gowarp") || cmd.equals("remwarp")) && args.length == 1) {
            for (Warp warp : warpService.allWarpsSorted()) {
                out.add(warp.name());
            }
            return filter(out, args[0]);
        }

        return out;
    }

    private List<String> filter(List<String> source, String input) {
        String needle = input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String entry : source) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(entry);
            }
        }
        return out;
    }
}
