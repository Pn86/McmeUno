package uno.mcme.pnplayertask;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PnPlayerTaskPlaceholder extends PlaceholderExpansion {
    private final PnPlayerTaskPlugin plugin;
    public PnPlayerTaskPlaceholder(PnPlayerTaskPlugin plugin) { this.plugin = plugin; }
    @Override public @NotNull String getIdentifier() { return "pnplayertask"; }
    @Override public @NotNull String getAuthor() { return "Pn86"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (!(offlinePlayer instanceof Player player)) return "";
        String key = params.toLowerCase();
        return switch (key) {
            case "taskyes" -> String.valueOf(plugin.getTaskManager().getCompletedCount(player));
            case "taskall" -> String.valueOf(plugin.getTaskManager().getTotalCount());
            case "taskno" -> String.valueOf(plugin.getTaskManager().getIncompleteCount(player));
            default -> {
                if (key.startsWith("tasktimeend_")) yield plugin.getTaskManager().getTaskRefreshDate(player, params.substring("tasktimeend_".length()));
                if (key.startsWith("tasktime_")) yield plugin.getTaskManager().getTaskRefreshRemaining(player, params.substring("tasktime_".length()));
                yield null;
            }
        };
    }
}
