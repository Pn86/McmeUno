package uno.mcme.pnplayertask.task;
import org.bukkit.Material;
import java.util.List;
public record PlayerTask(String id, Material item, String name, List<String> lore, List<TaskCondition> conditions, List<String> actions, RefreshRule refreshRule, String completeTitle, String completeSubtitle, String completeMessage) {}
