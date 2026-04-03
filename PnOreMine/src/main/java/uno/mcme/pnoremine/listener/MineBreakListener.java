package uno.mcme.pnoremine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.DropMode;
import uno.mcme.pnoremine.mine.MineRegion;
import uno.mcme.pnoremine.mine.OreEntry;

public class MineBreakListener implements Listener {

    private final PnOreMinePlugin plugin;

    public MineBreakListener(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        MineRegion mine = plugin.getMineManager().findMineByLocation(event.getBlockState().getLocation());
        if (mine == null) {
            return;
        }
        OreEntry ore = mine.getOreEntry(event.getBlockState().getType());
        if (ore == null) {
            return;
        }

        if (mine.getDropMode() == DropMode.VALUE || mine.getDropMode() == DropMode.ITEM) {
            event.getItems().clear();
        }
        plugin.rewardPlayerForOre(event.getPlayer(), mine, event.getBlockState().getType(), event.getBlockState().getLocation().add(0.5, 0.5, 0.5));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakFallback(BlockBreakEvent event) {
        if (event.isDropItems()) {
            return;
        }
        MineRegion mine = plugin.getMineManager().findMineByLocation(event.getBlock().getLocation());
        if (mine == null) {
            return;
        }
        plugin.rewardPlayerForOre(event.getPlayer(), mine, event.getBlock().getType(), event.getBlock().getLocation().add(0.5, 0.5, 0.5));
    }
}
