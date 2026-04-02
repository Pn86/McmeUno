package uno.mcme.pnoremine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import uno.mcme.pnoremine.PnOreMinePlugin;

import java.util.Map;

public class WorldProtectListener implements Listener {

    private final PnOreMinePlugin plugin;

    public WorldProtectListener(PnOreMinePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().hasPermission("pnoremine.bypass")) {
            return;
        }
        String worldName = event.getBlock().getWorld().getName().toLowerCase();
        if (!plugin.getMineManager().getMineWorlds().contains(worldName)) {
            return;
        }
        if (plugin.getMineManager().findMineByLocation(event.getBlock().getLocation()) != null) {
            return;
        }

        event.setCancelled(true);
        plugin.sendLocalized(event.getPlayer(), "world-protect", Map.of(), "[message]&c该世界受保护，只有矿区可破坏。");
    }
}
