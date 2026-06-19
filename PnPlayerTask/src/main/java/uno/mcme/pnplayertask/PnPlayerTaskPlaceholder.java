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
        return switch (params.toLowerCase()) {
            case "taskyes" -> String.valueOf(plugin.getTaskManager().getCompletedCount(player));
            case "taskall" -> String.valueOf(plugin.getTaskManager().getTotalCount());
            case "taskno" -> String.valueOf(plugin.getTaskManager().getIncompleteCount(player));
            case "tasktime" -> plugin.getTaskManager().getNextRefreshRemaining(player);
            case "tasktimeend" -> plugin.getTaskManager().getNextRefreshDate(player);
            default -> null;
        };
    }
}
