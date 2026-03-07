package com.pn86.pngoldprospecting;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class ProspectingListener implements Listener {
    private final PnGoldProspectingPlugin plugin;
    private final DataManager dataManager;

    public ProspectingListener(PnGoldProspectingPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }
        ProspectingBlock block = dataManager.getByLocation(clicked.getLocation());
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material requiredTool;
        try {
            requiredTool = Material.valueOf(plugin.getConfig().getString("defaults.required-tool", "BRUSH").toUpperCase());
        } catch (IllegalArgumentException ex) {
            requiredTool = Material.BRUSH;
        }
        if (hand.getType() != requiredTool) {
            return;
        }

        block.tickReset();
        if (block.isOpened()) {
            return;
        }

        event.setCancelled(true);

        Optional<LootEntry> rolled = dataManager.rollLoot(block);
        if (rolled.isEmpty()) {
            return;
        }

        LootEntry entry = rolled.get();
        if (entry.itemStack() != null && !entry.itemStack().getType().isAir()) {
            Item itemDrop = clicked.getWorld().dropItemNaturally(clicked.getLocation().add(0.5, 1, 0.5), entry.itemStack().clone());
            itemDrop.setOwner(player.getUniqueId());
        } else {
            Material displayMaterial = dataManager.getCommandDisplayMaterial();
            Item displayDrop = clicked.getWorld().dropItemNaturally(clicked.getLocation().add(0.5, 1, 0.5), new ItemStack(displayMaterial));
            displayDrop.setCanMobPickup(false);
            displayDrop.setUnlimitedLifetime(false);
            displayDrop.setPickupDelay(Integer.MAX_VALUE);
            Bukkit.getScheduler().runTaskLater(plugin, displayDrop::remove, 30L);
        }

        if (entry.isCommandLoot()) {
            String commandText = entry.command().replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandText);
        }

        clicked.getWorld().spawnParticle(Particle.FIREWORK, clicked.getLocation().add(0.5, 1, 0.5), 15, 0.3, 0.3, 0.3, 0.01);
        clicked.getWorld().playSound(clicked.getLocation(), Sound.BLOCK_SUSPICIOUS_SAND_BREAK, 1.0f, 1.2f);

        block.setOpened(true);
        block.setOpenedAtMillis(System.currentTimeMillis());
        dataManager.saveBlock(block);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (dataManager.getByLocation(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (dataManager.getByLocation(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFade(BlockFadeEvent event) {
        if (dataManager.getByLocation(event.getBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFlow(BlockFromToEvent event) {
        if (dataManager.getByLocation(event.getToBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> dataManager.getByLocation(block.getLocation()) != null);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> dataManager.getByLocation(block.getLocation()) != null);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> dataManager.getByLocation(block.getLocation()) != null)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> dataManager.getByLocation(block.getLocation()) != null)) {
            event.setCancelled(true);
        }
    }
}
