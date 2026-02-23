package uno.mcme.pnspeedlimit.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import uno.mcme.pnspeedlimit.SpeedLimitManager;
import uno.mcme.pnspeedlimit.model.SpeedType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeedLimitListener implements Listener {

    private final SpeedLimitManager manager;

    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Long> lastMoveAt = new HashMap<>();
    private final Map<UUID, Long> recentKnockback = new HashMap<>();
    private final Map<UUID, Long> recentExplosion = new HashMap<>();

    public SpeedLimitListener(SpeedLimitManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (!manager.isEnabled() || manager.isWhitelisted(player)) {
            lastLocation.put(uuid, to.clone());
            lastMoveAt.put(uuid, System.currentTimeMillis());
            return;
        }

        Location prev = lastLocation.get(uuid);
        long now = System.currentTimeMillis();
        Long prevAt = lastMoveAt.get(uuid);
        lastLocation.put(uuid, to.clone());
        lastMoveAt.put(uuid, now);

        if (prev == null || prevAt == null || prev.getWorld() == null || to.getWorld() == null || prev.getWorld() != to.getWorld()) {
            return;
        }

        long deltaMillis = now - prevAt;
        if (deltaMillis < 25L) {
            return;
        }

        double distance = prev.distance(to);
        if (distance <= 0.0D) {
            return;
        }

        double speed = distance / (deltaMillis / 1000.0D);
        SpeedType type = (player.isGliding() || player.isFlying()) ? SpeedType.FLY : SpeedType.MOVE;

        manager.updatePlayerSpeed(player, speed, type);
        manager.tryWarning(player, speed, type);

        double limit = manager.getSpeedLimit(type);
        if (speed > limit) {
            event.setTo(prev);
            manager.punish(player, speed, type);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            recentExplosion.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            recentKnockback.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        if (!manager.isEnabled() || manager.isWhitelisted(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long knockbackAge = now - recentKnockback.getOrDefault(uuid, 0L);
        long explosionAge = now - recentExplosion.getOrDefault(uuid, 0L);

        if (knockbackAge > 1200L && explosionAge > 1200L) {
            return;
        }

        Vector velocity = event.getVelocity();
        double speed = velocity.length() * 20.0D;

        SpeedType type = SpeedType.REPEL;
        manager.updatePlayerSpeed(player, speed, type);
        manager.tryWarning(player, speed, type);

        double limit = manager.getSpeedLimit(type);
        if (speed > limit) {
            Vector normalized = velocity.clone().normalize().multiply(limit / 20.0D);
            if (Double.isFinite(normalized.getX()) && Double.isFinite(normalized.getY()) && Double.isFinite(normalized.getZ())) {
                event.setVelocity(normalized);
            }
            manager.punish(player, speed, type);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastLocation.remove(uuid);
        lastMoveAt.remove(uuid);
        recentKnockback.remove(uuid);
        recentExplosion.remove(uuid);
    }
}
