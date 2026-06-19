package uno.mcme.pnplayertask.api;

import org.bukkit.entity.Player;
import uno.mcme.pnplayertask.task.PlayerTask;
import java.util.Collection;

public interface PnPlayerTaskApi {
    Collection<PlayerTask> getTasks();
    PlayerTask getTask(String id);
    boolean isComplete(Player player, String taskId);
    boolean isClaimed(Player player, String taskId);
    boolean claim(Player player, String taskId);
}
