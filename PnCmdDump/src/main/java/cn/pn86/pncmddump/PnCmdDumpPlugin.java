package cn.pn86.pncmddump;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class PnCmdDumpPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        if (name.equals("dump")) {
            return handleDump(sender);
        }

        if (name.equals("pncmddump")) {
            return handleMainCommand(sender, args);
        }

        return false;
    }

    private boolean handleDump(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(color(getConfig().getString("messages.player-only", "&c只有玩家可以使用该命令。")));
            return true;
        }

        if (!player.isOp() && !player.hasPermission("pncmddump.dump")) {
            player.sendMessage(color(getConfig().getString("messages.no-permission", "&c你没有权限使用这个命令。")));
            return true;
        }

        ItemStack inMainHand = player.getInventory().getItemInMainHand();
        if (inMainHand.getType() == Material.AIR || inMainHand.getAmount() <= 0) {
            player.sendMessage(color(getConfig().getString("messages.empty-hand", "&e你的主手没有物品。")));
            return true;
        }

        int multiplier = Math.max(1, getConfig().getInt("settings.multiplier", 1));
        int totalAmount = inMainHand.getAmount() * multiplier;
        int maxStackSize = inMainHand.getMaxStackSize();

        while (totalAmount > 0) {
            int stackAmount = Math.min(totalAmount, maxStackSize);
            ItemStack drop = inMainHand.clone();
            drop.setAmount(stackAmount);
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
            totalAmount -= stackAmount;
        }

        String success = getConfig().getString("messages.dump-success", "&a已复制主手物品并掉落到地面。倍率: &f%multiplier%x");
        player.sendMessage(color(success.replace("%multiplier%", String.valueOf(multiplier))));
        return true;
    }

    private boolean handleMainCommand(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp() && !sender.hasPermission("pncmddump.reload")) {
                sender.sendMessage(color(getConfig().getString("messages.no-permission", "&c你没有权限使用这个命令。")));
                return true;
            }

            reloadConfig();
            sender.sendMessage(color(getConfig().getString("messages.reload-success", "&aPnCmdDump 配置已重载。")));
            return true;
        }

        List<String> help = getConfig().getStringList("messages.help");
        if (help.isEmpty()) {
            sender.sendMessage(color("&e/pncmddump reload &7- 重载插件"));
        } else {
            for (String line : help) {
                sender.sendMessage(color(line));
            }
        }
        return true;
    }

    private String color(String text) {
        return text.replace('&', '§');
    }
}
