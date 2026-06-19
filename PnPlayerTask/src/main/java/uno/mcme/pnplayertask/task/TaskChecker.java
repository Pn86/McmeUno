package uno.mcme.pnplayertask.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uno.mcme.pnplayertask.PnPlayerTaskPlugin;
import uno.mcme.pnplayertask.util.Text;

public class TaskChecker {
    private final PnPlayerTaskPlugin plugin;
    private BukkitTask task;
    private long nextCheckMillis;

    public TaskChecker(PnPlayerTaskPlugin plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        long seconds = Math.max(1L, plugin.getConfig().getLong("settings.check-interval-seconds", 5L));
        nextCheckMillis = System.currentTimeMillis() + seconds * 1000L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkOnlinePlayers();
            nextCheckMillis = System.currentTimeMillis() + seconds * 1000L;
        }, seconds * 20L, seconds * 20L);
    }

    public void stop() { if (task != null) { task.cancel(); task = null; } }
    public long getNextCheckSeconds() { return Math.max(0L, (nextCheckMillis - System.currentTimeMillis() + 999L) / 1000L); }

    public void checkOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getTaskManager().refreshDueTasks(player);
            for (PlayerTask playerTask : plugin.getTaskManager().getTasks()) {
                if (plugin.getTaskManager().isCompleted(player.getUniqueId(), playerTask.id()) || plugin.getTaskManager().isClaimed(player.getUniqueId(), playerTask.id())) continue;
                if (plugin.getTaskManager().isConditionComplete(player, playerTask.id())) {
                    plugin.getTaskManager().markCompleted(player, playerTask);
                    notifyComplete(player, playerTask);
                }
            }
        }
    }

    private void notifyComplete(Player player, PlayerTask task) {
        String title = task.completeTitle().isBlank() ? plugin.getConfig().getString("messages.complete-title", "%task_name%") : task.completeTitle();
        String subtitle = task.completeSubtitle().isBlank() ? plugin.getConfig().getString("messages.complete-subtitle", "&a任务已完成！输入 /task 领取奖励") : task.completeSubtitle();
        String message = task.completeMessage().isBlank() ? plugin.getConfig().getString("messages.complete-message", "&a任务 &e%task_name% &a已完成！输入 &e/task &a打开页面领取奖励。") : task.completeMessage();
        title = replace(player, task, title);
        subtitle = replace(player, task, subtitle);
        message = replace(player, task, message);
        player.sendTitle(Text.color(title), Text.color(subtitle), 10, 70, 20);
        player.sendMessage(Text.color(plugin.getConfig().getString("messages.prefix", "") + message));
    }

    private String replace(Player player, PlayerTask task, String text) {
        return Text.papi(player, text == null ? "" : text)
                .replace("%task_id%", task.id())
                .replace("%task_name%", Text.color(task.name()))
                .replace("%player_name%", player.getName())
                .replace("%player%", player.getName());
    }
}
