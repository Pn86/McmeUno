package uno.mcme.pnplayertask;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import uno.mcme.pnplayertask.api.PnPlayerTaskApi;
import uno.mcme.pnplayertask.command.TaskCommands;
import uno.mcme.pnplayertask.gui.TaskGui;
import uno.mcme.pnplayertask.listener.TaskListener;
import uno.mcme.pnplayertask.task.PlayerTask;
import uno.mcme.pnplayertask.task.PlayerTaskManager;
import uno.mcme.pnplayertask.task.TaskChecker;
import uno.mcme.pnplayertask.util.Text;

import java.util.Collection;

public class PnPlayerTaskPlugin extends JavaPlugin implements PnPlayerTaskApi {
    private PlayerTaskManager taskManager;
    private TaskGui taskGui;
    private TaskChecker taskChecker;

    @Override public void onEnable() {
        saveDefaultConfig();
        taskManager = new PlayerTaskManager(this);
        taskGui = new TaskGui(this);
        taskChecker = new TaskChecker(this);
        reloadAll();
        TaskCommands commands = new TaskCommands(this);
        getCommand("task").setExecutor(commands);
        getCommand("pnplayertask").setExecutor(commands);
        getCommand("pnplayertask").setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(new TaskListener(this), this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) new PnPlayerTaskPlaceholder(this).register();
    }

    @Override public void onDisable() {
        if (taskChecker != null) taskChecker.stop();
        if (taskManager != null) taskManager.savePlayers();
    }

    public void reloadAll() {
        reloadConfig();
        taskManager.load();
        taskChecker.start();
    }

    public PlayerTaskManager getTaskManager() { return taskManager; }
    public TaskGui getTaskGui() { return taskGui; }
    public TaskChecker getTaskChecker() { return taskChecker; }

    public void msg(CommandSender sender, String key, String... replacements) {
        String text = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) text = text.replace(replacements[i], replacements[i + 1]);
        sender.sendMessage(Text.color(getConfig().getString("messages.prefix", "") + text));
    }

    @Override public Collection<PlayerTask> getTasks() { return taskManager.getTasks(); }
    @Override public PlayerTask getTask(String id) { return taskManager.getTask(id); }
    @Override public boolean isComplete(Player player, String taskId) { return taskManager.isComplete(player, taskId); }
    @Override public boolean isClaimed(Player player, String taskId) { return taskManager.isClaimed(player.getUniqueId(), taskId); }

    @Override public boolean claim(Player player, String taskId) {
        PlayerTask task = taskManager.getTask(taskId);
        if (task == null) { msg(player, "task-not-found"); return false; }
        if (taskManager.isClaimed(player.getUniqueId(), taskId)) { msg(player, "already-claimed"); return false; }
        if (!taskManager.isComplete(player, taskId)) { msg(player, "not-complete"); return false; }
        if (!taskManager.isCompleted(player.getUniqueId(), taskId)) taskManager.markCompleted(player, task);
        taskManager.runActions(player, task);
        taskManager.setClaimed(player.getUniqueId(), player.getName(), taskId);
        msg(player, "claim-success", "%task_name%", Text.color(task.name()));
        return true;
    }
}
