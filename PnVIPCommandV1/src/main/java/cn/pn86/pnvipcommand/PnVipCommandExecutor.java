package cn.pn86.pnvipcommand;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WeatherType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class PnVipCommandExecutor implements CommandExecutor, TabCompleter {

    private final PnVIPCommandV1Plugin plugin;

    public PnVipCommandExecutor(PnVIPCommandV1Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);
        return switch (cmd) {
            case "pnvc" -> handleReload(sender, args);
            case "ptp" -> handlePtp(sender, args);
            case "pgive" -> handleGive(sender, args);
            case "ptime" -> handleTime(sender, args);
            case "pweather" -> handleWeather(sender, args);
            case "pexp" -> handleExp(sender);
            default -> false;
        };
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pnvipcommand.reload")) {
            plugin.sendConfigMessage(sender, "no_permission");
            return true;
        }
        if (args.length != 1 || !"reload".equalsIgnoreCase(args[0])) {
            plugin.sendConfigMessage(sender, "pnvc_usage");
            return true;
        }

        plugin.reloadPluginConfig();
        plugin.sendConfigMessage(sender, "reload_success");
        return true;
    }

    private boolean handlePtp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendConfigMessage(sender, "player_only");
            return true;
        }
        if (!player.hasPermission("pnvipcommand.tp")) {
            plugin.sendConfigMessage(player, "no_permission");
            return true;
        }
        if (args.length != 1) {
            plugin.sendConfigMessage(player, "ptp_usage");
            return true;
        }

        long remaining = plugin.getCooldownManager().getRemaining("ptp", player.getUniqueId());
        if (remaining > 0) {
            plugin.sendConfigMessage(player, "cooldown", "%seconds%", String.valueOf(remaining));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            plugin.sendConfigMessage(player, "target_offline");
            return true;
        }

        Set<String> whitelist = plugin.getStringList("tp_player_whitelist")
                .stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (whitelist.contains(target.getName().toLowerCase(Locale.ROOT))) {
            plugin.sendConfigMessage(player, "ptp_target_whitelisted");
            return true;
        }

        player.teleport(target.getLocation());
        plugin.getCooldownManager().markUsed("ptp", player.getUniqueId());
        plugin.sendConfigMessage(player, "ptp_success", "%player%", target.getName());
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendConfigMessage(sender, "player_only");
            return true;
        }
        if (!player.hasPermission("pnvipcommand.give")) {
            plugin.sendConfigMessage(player, "no_permission");
            return true;
        }
        if (args.length != 1) {
            plugin.sendConfigMessage(player, "pgive_usage");
            return true;
        }

        long remaining = plugin.getCooldownManager().getRemaining("pgive", player.getUniqueId());
        if (remaining > 0) {
            plugin.sendConfigMessage(player, "cooldown", "%seconds%", String.valueOf(remaining));
            return true;
        }

        String itemId = args[0].toUpperCase(Locale.ROOT);
        if (itemId.contains("{") || itemId.contains("[") || itemId.contains(":") || itemId.contains(" ")) {
            plugin.sendConfigMessage(player, "pgive_simple_id_only");
            return true;
        }

        Material material = Material.matchMaterial(itemId);
        if (material == null || material.isAir()) {
            plugin.sendConfigMessage(player, "invalid_item");
            return true;
        }

        Set<String> blacklist = plugin.getStringList("give_item_blacklist")
                .stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        if (blacklist.contains(material.name())) {
            plugin.sendConfigMessage(player, "pgive_item_blacklisted");
            return true;
        }

        player.getInventory().addItem(new ItemStack(material, 64));
        plugin.getCooldownManager().markUsed("pgive", player.getUniqueId());
        plugin.sendConfigMessage(player, "pgive_success", "%item%", material.name());
        return true;
    }

    private boolean handleTime(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendConfigMessage(sender, "player_only");
            return true;
        }
        if (!player.hasPermission("pnvipcommand.time")) {
            plugin.sendConfigMessage(player, "no_permission");
            return true;
        }
        if (args.length != 1) {
            plugin.sendConfigMessage(player, "ptime_usage");
            return true;
        }

        long remaining = plugin.getCooldownManager().getRemaining("ptime", player.getUniqueId());
        if (remaining > 0) {
            plugin.sendConfigMessage(player, "cooldown", "%seconds%", String.valueOf(remaining));
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        World world = player.getWorld();
        if (!isWorldAllowed("time_world_whitelist", world)) {
            plugin.sendConfigMessage(player, "ptime_world_not_allowed", "%world%", world.getName());
            return true;
        }
        switch (type) {
            case "day" -> world.setTime(1000);
            case "night" -> world.setTime(13000);
            default -> {
                plugin.sendConfigMessage(player, "ptime_usage");
                return true;
            }
        }

        plugin.getCooldownManager().markUsed("ptime", player.getUniqueId());
        plugin.sendConfigMessage(player, "ptime_success", "%type%", type);
        return true;
    }

    private boolean handleWeather(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendConfigMessage(sender, "player_only");
            return true;
        }
        if (!player.hasPermission("pnvipcommand.weather")) {
            plugin.sendConfigMessage(player, "no_permission");
            return true;
        }
        if (args.length != 1) {
            plugin.sendConfigMessage(player, "pweather_usage");
            return true;
        }

        long remaining = plugin.getCooldownManager().getRemaining("pweather", player.getUniqueId());
        if (remaining > 0) {
            plugin.sendConfigMessage(player, "cooldown", "%seconds%", String.valueOf(remaining));
            return true;
        }

        World world = player.getWorld();
        if (!isWorldAllowed("weather_world_whitelist", world)) {
            plugin.sendConfigMessage(player, "pweather_world_not_allowed", "%world%", world.getName());
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        switch (type) {
            case "clear" -> {
                world.setStorm(false);
                world.setThundering(false);
                player.setPlayerWeather(WeatherType.CLEAR);
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
                player.setPlayerWeather(WeatherType.DOWNFALL);
            }
            case "thunder" -> {
                world.setStorm(true);
                world.setThundering(true);
                player.setPlayerWeather(WeatherType.DOWNFALL);
            }
            default -> {
                plugin.sendConfigMessage(player, "pweather_usage");
                return true;
            }
        }

        plugin.getCooldownManager().markUsed("pweather", player.getUniqueId());
        plugin.sendConfigMessage(player, "pweather_success", "%type%", type);
        return true;
    }


    private boolean isWorldAllowed(String path, World world) {
        List<String> whitelist = plugin.getStringList(path);
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        String worldName = world.getName().toLowerCase(Locale.ROOT);
        return whitelist.stream().anyMatch(w -> w != null && w.toLowerCase(Locale.ROOT).equals(worldName));
    }

    private boolean handleExp(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.sendConfigMessage(sender, "player_only");
            return true;
        }
        if (!player.hasPermission("pnvipcommand.exp")) {
            plugin.sendConfigMessage(player, "no_permission");
            return true;
        }

        long remaining = plugin.getCooldownManager().getRemaining("pexp", player.getUniqueId());
        if (remaining > 0) {
            plugin.sendConfigMessage(player, "cooldown", "%seconds%", String.valueOf(remaining));
            return true;
        }

        player.giveExpLevels(1000);
        plugin.getCooldownManager().markUsed("pexp", player.getUniqueId());
        plugin.sendConfigMessage(player, "pexp_success", "%levels%", "1000");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmd = command.getName().toLowerCase(Locale.ROOT);

        if (cmd.equals("pnvc")) {
            if (args.length == 1) {
                return partial(args[0], Collections.singletonList("reload"));
            }
            return Collections.emptyList();
        }

        if (cmd.equals("ptp") && args.length == 1) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return partial(args[0], names);
        }

        if (cmd.equals("pgive") && args.length == 1) {
            List<String> materials = Arrays.stream(Material.values())
                    .filter(m -> !m.isAir())
                    .map(Material::name)
                    .collect(Collectors.toList());
            return partial(args[0].toUpperCase(Locale.ROOT), materials);
        }

        if (cmd.equals("ptime") && args.length == 1) {
            return partial(args[0], Arrays.asList("day", "night"));
        }

        if (cmd.equals("pweather") && args.length == 1) {
            return partial(args[0], Arrays.asList("clear", "rain", "thunder"));
        }

        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> source) {
        String low = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String s : source) {
            if (s.toLowerCase(Locale.ROOT).startsWith(low)) {
                result.add(s);
            }
        }
        return result;
    }
}
