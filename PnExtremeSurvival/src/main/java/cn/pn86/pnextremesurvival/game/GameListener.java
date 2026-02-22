package cn.pn86.pnextremesurvival.game;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        gameManager.onJoin(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (gameManager.isParticipating(player)) {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player damager)) return;
        if (victim.getScoreboard().getEntryTeam(victim.getName()) != null
                && victim.getScoreboard().getEntryTeam(victim.getName()) == victim.getScoreboard().getEntryTeam(damager.getName())) {
            event.setCancelled(true);
        }
    }
}
