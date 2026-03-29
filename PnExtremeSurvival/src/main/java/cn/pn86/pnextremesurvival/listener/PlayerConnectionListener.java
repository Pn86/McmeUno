package cn.pn86.pnextremesurvival.listener;

import cn.pn86.pnextremesurvival.service.LimitedLifeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerConnectionListener implements Listener {

    private final LimitedLifeService limitedLifeService;

    public PlayerConnectionListener(LimitedLifeService limitedLifeService) {
        this.limitedLifeService = limitedLifeService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        limitedLifeService.loadPlayer(event.getPlayer());
    }
}
