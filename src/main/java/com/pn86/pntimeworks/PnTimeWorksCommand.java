package com.pn86.pntimeworks;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class PnTimeWorksCommand implements CommandExecutor, TabCompleter {
    private final PnTimeWorksPlugin plugin;

    public PnTimeWorksCommand(PnTimeWorksPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pntimeworks.admin")) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.message("usage"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(plugin.message("reload-success"));
                return true;
            }
            case "list" -> {
                List<WorkGroup> groups = plugin.getWorkGroups();
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("count", String.valueOf(groups.size()));
                sender.sendMessage(plugin.message("list-header", placeholders));

                for (WorkGroup group : groups) {
                    List<String> previewTimes = group.rules().stream()
                            .map(TimeRule::raw)
                            .collect(Collectors.toList());
                    String line = "&7- &e" + group.id()
                            + " &f| &btime: &f" + previewTimes
                            + " &f| &aaction: &f" + group.commands().size();
                    sender.sendMessage(line.replace('&', '§'));
                }
                return true;
            }
            default -> {
                sender.sendMessage(plugin.message("usage"));
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("reload", "list");
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(input)) {
                    result.add(option);
                }
            }
            return result;
        }
        return List.of();
    }
}
