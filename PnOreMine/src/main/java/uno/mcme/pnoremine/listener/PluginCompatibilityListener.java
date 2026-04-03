package uno.mcme.pnoremine.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.MineRegion;

import java.util.Iterator;

public class PluginCompatibilityListener implements Listener {

    private final PnOreMinePlugin plugin;

    public PluginCompatibilityListener(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getPlayer().hasPermission("pnoremine.bypass")) {
            return;
        }
        String worldName = event.getBlock().getWorld().getName().toLowerCase();
        if (!plugin.getMineManager().getMineWorlds().contains(worldName)) {
            return;
        }
        if (plugin.getMineManager().findMineByLocation(event.getBlock().getLocation()) == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        processExplodedBlocks(event.blockList().iterator(), null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Player source = findPlayerSource(event.getEntity());
        processExplodedBlocks(event.blockList().iterator(), source);
    }

    private void processExplodedBlocks(Iterator<Block> iterator, Player source) {
        while (iterator.hasNext()) {
            Block block = iterator.next();
            String worldName = block.getWorld().getName().toLowerCase();
            if (!plugin.getMineManager().getMineWorlds().contains(worldName)) {
                continue;
            }
            MineRegion mine = plugin.getMineManager().findMineByLocation(block.getLocation());
            if (mine == null) {
                iterator.remove();
                continue;
            }
            if (source != null) {
                plugin.rewardPlayerForOre(source, mine, block.getType(), center(block.getLocation()));
            }
        }
    }

    private Location center(Location location) {
        return location.clone().add(0.5, 0.5, 0.5);
    }

    private Player findPlayerSource(Entity entity) {
        if (entity instanceof TNTPrimed primed && primed.getSource() instanceof Player player) {
            return player;
        }
        return null;
    }
}
