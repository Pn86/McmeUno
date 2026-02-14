package cn.pn86.pnlevel;

import cn.pn86.pnlevel.gui.GuiManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PnLevelListener implements Listener {
    private final PnLevelPlugin plugin;

    public PnLevelListener(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var data = plugin.getPlayerDataManager().getOrCreate(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        data.setLastName(event.getPlayer().getName());
        plugin.getLevelManager().processLevelUps(data, event.getPlayer(), true);
        plugin.getExpManager().onJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getExpManager().onQuit(event.getPlayer());
    }

    @EventHandler
    public void onKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        plugin.getExpManager().triggerKill(event.getEntity().getKiller(), event.getEntityType().getKey().asString());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        plugin.getExpManager().triggerDestroy(event.getPlayer(), event.getBlock().getType().getKey().asString());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        plugin.getExpManager().triggerPlace(event.getPlayer(), event.getBlockPlaced().getType().getKey().asString());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof GuiManager.MenuHolder holder)) return;

        // Prevent item moving/stealing/placing into custom menus.
        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        if (holder.type() == GuiManager.MenuType.GIFT) {
            plugin.getGuiManager().handleGiftClick(player, holder, event.getRawSlot());
        } else if (holder.type() == GuiManager.MenuType.TOP) {
            plugin.getGuiManager().handleTopClick(player, event.getRawSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof GuiManager.MenuHolder) {
            event.setCancelled(true);
        }
    }
}
