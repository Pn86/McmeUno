package uno.mcme.pnoremine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.MineRegion;

public class MineBreakListener implements Listener {

    private final PnOreMinePlugin plugin;

    public MineBreakListener(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        MineRegion mine = plugin.getMineManager().findMineByLocation(event.getBlock().getLocation());
        if (mine == null) {
            return;
        }
        boolean rewarded = plugin.rewardPlayerForOre(event.getPlayer(), mine, event.getBlock().getType(), event.getBlock().getLocation());
        if (rewarded) {
            event.setDropItems(false);
        }
    }
}
