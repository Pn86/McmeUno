package cn.pn86.pnbooknews.listener;

import cn.pn86.pnbooknews.PnBookNewsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinBookListener implements Listener {

    private final PnBookNewsPlugin plugin;

    public JoinBookListener(PnBookNewsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.shouldShowOnJoin()) {
            return;
        }
        if (plugin.isAuthMeCompatEnabled() && plugin.isAuthMeHooked()) {
            return;
        }
        plugin.openNewsBookLater(event.getPlayer());
    }
}
