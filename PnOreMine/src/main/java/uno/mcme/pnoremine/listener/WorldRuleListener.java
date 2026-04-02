package uno.mcme.pnoremine.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.MineRegion;

import java.util.Map;

public class WorldRuleListener implements Listener {

    private final PnOreMinePlugin plugin;

    public WorldRuleListener(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        String worldName = victim.getWorld().getName();
        if (!worldName.equalsIgnoreCase(attacker.getWorld().getName())) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.getMineManager().getMineWorlds().contains(worldName.toLowerCase())) {
            return;
        }

        boolean allow = plugin.getMineManager().canPvp(victim.getLocation())
            && plugin.getMineManager().canPvp(attacker.getLocation());
        if (!allow) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        String worldName = event.getRespawnLocation().getWorld() == null ? "" : event.getRespawnLocation().getWorld().getName();
        MineRegion primary = plugin.getMineManager().getWorldPrimaryMine(worldName);
        if (primary == null) {
            return;
        }
        Location spawn = primary.getSpawnLocation();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
            plugin.sendLocalized(event.getPlayer(), "world-spawn-teleport", Map.of(), "[actionbar]&e你已被传送到矿区出生点。");
        }
    }
}
