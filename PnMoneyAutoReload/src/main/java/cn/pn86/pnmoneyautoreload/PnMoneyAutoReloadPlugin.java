package cn.pn86.pnmoneyautoreload;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class PnMoneyAutoReloadPlugin extends JavaPlugin {

    private BukkitTask autoReloadTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        startAutoReloadTask();
        getLogger().info("PnMoneyAutoReload enabled.");
    }

    @Override
    public void onDisable() {
        cancelAutoReloadTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("pnmar")) {
            return false;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.isOp() && !sender.hasPermission("pnmar.reload")) {
                sender.sendMessage(color(getConfig().getString("messages.no-permission", "&c你没有权限执行该命令。")));
                return true;
            }

            reloadConfig();
            startAutoReloadTask();
            sender.sendMessage(color(getConfig().getString("messages.reload-success", "&aPnMoneyAutoReload 已重载。")));
            return true;
        }

        for (String line : getConfig().getStringList("messages.help")) {
            sender.sendMessage(color(line));
        }
        return true;
    }

    private void startAutoReloadTask() {
        cancelAutoReloadTask();

        long intervalSeconds = Math.max(1L, getConfig().getLong("time", 60L));
        long intervalTicks = intervalSeconds * 20L;
        String commandToRun = sanitizeCommand(getConfig().getString("command", "pnmy reload"));

        autoReloadTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
            if (!success) {
                getLogger().warning("自动执行命令失败: /" + commandToRun);
            }
        }, intervalTicks, intervalTicks);

        getLogger().info("自动重载任务已启动，间隔 " + intervalSeconds + " 秒，命令 /" + commandToRun);
    }

    private void cancelAutoReloadTask() {
        if (autoReloadTask != null) {
            autoReloadTask.cancel();
            autoReloadTask = null;
        }
    }

    private String sanitizeCommand(String command) {
        String safe = command == null ? "pnmy reload" : command.trim();
        if (safe.startsWith("/")) {
            safe = safe.substring(1);
        }
        if (safe.isEmpty()) {
            safe = "pnmy reload";
        }
        return safe;
    }

    private String color(String text) {
        return text.replace('&', '§');
    }
}
