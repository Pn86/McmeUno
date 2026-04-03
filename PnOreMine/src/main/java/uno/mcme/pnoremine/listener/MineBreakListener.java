package uno.mcme.pnoremine.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.DropMode;
import uno.mcme.pnoremine.mine.MineRegion;
import uno.mcme.pnoremine.mine.OreEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MineBreakListener implements Listener {

    private final PnOreMinePlugin plugin;
    private final Map<String, PendingReward> pendingRewards = new HashMap<>();

    public MineBreakListener(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakPrepare(BlockBreakEvent event) {
        MineRegion mine = plugin.getMineManager().findMineByLocation(event.getBlock().getLocation());
        if (mine == null) {
            return;
        }

        OreEntry ore = mine.getOreEntry(event.getBlock().getType());
        if (ore == null) {
            return;
        }

        if (mine.getDropMode() == DropMode.VALUE || mine.getDropMode() == DropMode.ITEM) {
            event.setDropItems(false);
        }

        String key = key(event.getPlayer().getUniqueId(), event.getBlock().getLocation());
        pendingRewards.put(key, new PendingReward(mine, event.getBlock().getType(), event.getBlock().getLocation().add(0.5, 0.5, 0.5)));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBreakFinalize(BlockBreakEvent event) {
        String key = key(event.getPlayer().getUniqueId(), event.getBlock().getLocation());
        PendingReward pending = pendingRewards.remove(key);
        if (pending == null) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        plugin.rewardPlayerForOre(event.getPlayer(), pending.mine(), pending.blockType(), pending.location());
    }

    private String key(UUID uuid, Location location) {
        return uuid + ":" + location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record PendingReward(MineRegion mine, Material blockType, Location location) {
    }
}
