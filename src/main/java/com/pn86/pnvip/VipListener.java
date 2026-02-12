package com.pn86.pnvip;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VipListener implements Listener {
    private final PnVipPlugin plugin;

    public VipListener(PnVipPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getVipManager().applyPermissions(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getVipManager().clearAttachment(event.getPlayer().getUniqueId());
    }
}
