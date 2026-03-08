package cn.pn86.pnwarp.gui;

import cn.pn86.pnwarp.model.Warp;
import cn.pn86.pnwarp.service.WarpService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class WarpGuiListener implements Listener {
    private final WarpGuiManager guiManager;
    private final WarpService warpService;

    public WarpGuiListener(WarpGuiManager guiManager, WarpService warpService) {
        this.guiManager = guiManager;
        this.warpService = warpService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().equals(guiManager.title())) {
            return;
        }

        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == guiManager.previousSlot()) {
            guiManager.openPrev(player);
            return;
        }
        if (slot == guiManager.nextSlot()) {
            guiManager.openNext(player);
            return;
        }
        if (slot == guiManager.closeSlot()) {
            player.closeInventory();
            return;
        }

        if (slot < 0 || slot >= 45) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null || !clicked.getItemMeta().hasDisplayName()) {
            return;
        }

        String warpName = clicked.getItemMeta().hasItemName()
                ? clicked.getItemMeta().getItemName()
                : org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (warpName == null || warpName.isBlank()) {
            return;
        }

        Optional<Warp> warp = warpService.getWarp(warpName);
        if (warp.isEmpty()) {
            return;
        }

        player.closeInventory();
        warpService.scheduleTeleport(player, warp.get());
    }
}
