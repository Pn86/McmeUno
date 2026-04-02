package uno.mcme.pnoremine.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import uno.mcme.pnoremine.PnOreMinePlugin;
import uno.mcme.pnoremine.mine.DropMode;
import uno.mcme.pnoremine.mine.MineRegion;
import uno.mcme.pnoremine.mine.OreEntry;

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

        OreEntry ore = mine.getOreEntry(event.getBlock().getType());
        if (ore == null) {
            return;
        }

        Player player = event.getPlayer();
        if (mine.getDropMode() == DropMode.ITEM) {
            event.setDropItems(false);
            int amount = Math.max(1, (int) Math.floor(ore.amount()));
            Material material = ore.material();
            player.getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(material, amount));
            return;
        }

        event.setDropItems(false);
        plugin.getEconomy().depositPlayer(player, ore.amount());
        player.sendActionBar(plugin.msg("reward-value").replace("%value%", String.valueOf(ore.amount())));
    }
}
