package uno.mcme.pnplayertask.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import uno.mcme.pnplayertask.PnPlayerTaskPlugin;
import uno.mcme.pnplayertask.task.PlayerTask;

public class TaskListener implements Listener {
    private final PnPlayerTaskPlugin plugin;
    public TaskListener(PnPlayerTaskPlugin plugin) { this.plugin = plugin; }
    @EventHandler public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
        String id = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(plugin.getTaskGui().key(), PersistentDataType.STRING);
        if (id == null) return; e.setCancelled(true);
        if (id.startsWith("__close")) { p.closeInventory(); return; }
        if (id.startsWith("__previous") || id.startsWith("__next")) { int page = Integer.parseInt(id.substring(id.indexOf(':') + 1)); plugin.getTaskGui().open(p, Math.max(0, page + (id.startsWith("__next") ? 1 : -1))); return; }
        plugin.claim(p, id);
    }
    @EventHandler public void onConsume(PlayerItemConsumeEvent e) { add(e.getPlayer(), e.getItem(), "use_item"); }
    @EventHandler public void onPickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p) add(p, e.getItem().getItemStack(), "get_item"); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { add(e.getPlayer(), e.getItemDrop().getItemStack(), "drop_item"); }
    private void add(Player player, ItemStack stack, String type) { plugin.getTaskManager().addProgress(player, stack.getType().name().toLowerCase(), type, stack.getAmount()); }
}
