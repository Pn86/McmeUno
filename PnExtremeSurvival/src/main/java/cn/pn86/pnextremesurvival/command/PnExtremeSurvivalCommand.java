package cn.pn86.pnextremesurvival.command;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import cn.pn86.pnextremesurvival.service.LimitedLifeService;
import cn.pn86.pnextremesurvival.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PnExtremeSurvivalCommand implements CommandExecutor, TabCompleter {

    private final PnExtremeSurvivalPlugin plugin;
    private final LimitedLifeService limitedLifeService;

    public PnExtremeSurvivalCommand(PnExtremeSurvivalPlugin plugin, LimitedLifeService limitedLifeService) {
        this.plugin = plugin;
        this.limitedLifeService = limitedLifeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.send(plugin, sender, "usage");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("see")) {
            return handleSee(sender, args);
        }

        if (!sender.hasPermission("pnes.admin")) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        return switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                MessageUtil.send(plugin, sender, "reload");
                yield true;
            }
            case "add" -> handleModify(sender, args, true);
            case "remove" -> handleModify(sender, args, false);
            case "spawn" -> handleSpawn(sender, args);
            default -> {
                MessageUtil.send(plugin, sender, "usage");
                yield true;
            }
        };
    }

    private boolean handleSee(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pnes.see") && !sender.hasPermission("pnes.admin")) {
            MessageUtil.send(plugin, sender, "no-permission");
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.send(plugin, sender, "player-not-found");
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            MessageUtil.send(plugin, sender, "usage");
            return true;
        }

        String msg = MessageUtil.format(plugin, "see")
                .replace("%player%", target.getName())
                .replace("%health%", String.format(Locale.US, "%.1f", limitedLifeService.getPlayerMaxHealth(target)));
        MessageUtil.sendRaw(sender, msg);
        return true;
    }

    private boolean handleModify(CommandSender sender, String[] args, boolean add) {
        if (args.length < 3) {
            MessageUtil.send(plugin, sender, "usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(plugin, sender, "player-not-found");
            return true;
        }

        double hearts;
        try {
            hearts = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            MessageUtil.send(plugin, sender, "invalid-number");
            return true;
        }

        double healthAmount = hearts * 2.0;
        if (add) {
            limitedLifeService.addHealth(target, healthAmount);
        } else {
            limitedLifeService.addHealth(target, -healthAmount);
        }

        String msg = MessageUtil.format(plugin, "changed")
                .replace("%player%", target.getName())
                .replace("%health%", String.format(Locale.US, "%.1f", limitedLifeService.getPlayerMaxHealth(target)));
        MessageUtil.sendRaw(sender, msg);
        return true;
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(plugin, sender, "usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.send(plugin, sender, "player-not-found");
            return true;
        }

        limitedLifeService.reviveToOneHeart(target);
        MessageUtil.send(plugin, sender, "revived");
        MessageUtil.send(plugin, target, "revived-target");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "see", "add", "remove", "spawn"), args[0]);
        }

        if (args.length == 2 && List.of("see", "add", "remove", "spawn").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String prefix) {
        List<String> matched = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                matched.add(value);
            }
        }
        return matched;
    }
}
