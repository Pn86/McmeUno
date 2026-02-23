package uno.mcme.pnspeedlimit.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uno.mcme.pnspeedlimit.PnSpeedLimitPlugin;
import uno.mcme.pnspeedlimit.SpeedLimitManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PnslCommand implements CommandExecutor, TabCompleter {

    private final PnSpeedLimitPlugin plugin;
    private final SpeedLimitManager manager;

    public PnslCommand(PnSpeedLimitPlugin plugin, SpeedLimitManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(msg("messages.help"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "whitelist" -> handleWhitelist(sender, args);
            case "limit" -> handleLimit(sender, args);
            case "use" -> handleUse(sender, args);
            case "reload" -> {
                manager.reload();
                sender.sendMessage(msg("messages.reload"));
            }
            default -> sender.sendMessage(msg("messages.help"));
        }
        return true;
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(msg("messages.whitelist-usage"));
            return;
        }

        String action = args[1].toLowerCase(Locale.ROOT);
        String target = args[2];
        boolean changed = false;
        if (action.equals("add")) {
            changed = manager.addWhitelist(target);
            sender.sendMessage(format("messages.whitelist-added", "%target%", target));
        } else if (action.equals("remove")) {
            changed = manager.removeWhitelist(target);
            sender.sendMessage(format("messages.whitelist-removed", "%target%", target));
        } else {
            sender.sendMessage(msg("messages.whitelist-usage"));
            return;
        }

        if (changed) {
            manager.save();
        }
    }

    private void handleLimit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(msg("messages.limit-usage"));
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(msg("messages.invalid-number"));
            return;
        }

        switch (type) {
            case "all" -> manager.setLimitAll(value);
            case "move" -> manager.setLimitMove(value);
            case "fly" -> manager.setLimitFly(value);
            case "repel" -> manager.setLimitRepel(value);
            default -> {
                sender.sendMessage(msg("messages.limit-usage"));
                return;
            }
        }

        manager.save();
        sender.sendMessage(format(msg("messages.limit-updated"), "%type%", type, "%value%", String.format(Locale.US, "%.2f", value)));
    }

    private void handleUse(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg("messages.use-usage"));
            return;
        }

        String state = args[1].toLowerCase(Locale.ROOT);
        if (!state.equals("on") && !state.equals("off")) {
            sender.sendMessage(msg("messages.use-usage"));
            return;
        }

        boolean enabled = state.equals("on");
        manager.setEnabled(enabled);
        manager.save();
        sender.sendMessage(enabled ? msg("messages.use-on") : msg("messages.use-off"));
    }

    private String msg(String path) {
        return manager.colorize(plugin.getConfig().getString(path, path));
    }

    private String format(String path, String placeholder, String value) {
        return msg(path).replace(placeholder, value);
    }

    private String format(String text, String... kv) {
        String out = text;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out = out.replace(kv[i], kv[i + 1]);
        }
        return out;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("whitelist", "limit", "use", "reload"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("whitelist")) {
                return filter(Arrays.asList("add", "remove"), args[1]);
            }
            if (args[0].equalsIgnoreCase("limit")) {
                return filter(Arrays.asList("all", "move", "fly", "repel"), args[1]);
            }
            if (args[0].equalsIgnoreCase("use")) {
                return filter(Arrays.asList("on", "off"), args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> source, String input) {
        List<String> out = new ArrayList<>();
        String lower = input.toLowerCase(Locale.ROOT);
        for (String s : source) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(s);
            }
        }
        return out;
    }
}
