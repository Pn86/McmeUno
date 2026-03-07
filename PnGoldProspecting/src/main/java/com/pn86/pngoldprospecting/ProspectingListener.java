package com.pn86.pngoldprospecting;

import org.bukkit.Bukkit;
import org.bukkit.Color;
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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ProspectingListener implements Listener {
    private final PnGoldProspectingPlugin plugin;
    private final DataManager dataManager;
    private final Map<UUID, BukkitTask> brushingTasks = new HashMap<>();
    private final Map<String, UUID> blockBusyBy = new HashMap<>();

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

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!isHoldingRequiredTool(player)) {
            return;
        }

        block.tickReset();
        if (block.isOpened()) {
            return;
        }

        String key = blockKey(block.getLocation());
        UUID current = blockBusyBy.get(key);
        if (current != null && !current.equals(player.getUniqueId())) {
            return;
        }
        if (brushingTasks.containsKey(player.getUniqueId())) {
            return;
        }

        startBrushing(player, block);
    }

    private void startBrushing(Player player, ProspectingBlock block) {
        UUID playerId = player.getUniqueId();
        String key = blockKey(block.getLocation());
        blockBusyBy.put(key, playerId);

        int durationTicks = Math.max(20, plugin.getConfig().getInt("defaults.brushing-duration-ticks", 40));
        int period = 2;
        int needSteps = Math.max(1, durationTicks / period);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int step = 0;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(playerId);
                if (p == null || !p.isOnline()) {
                    stop(false);
                    return;
                }

                if (!isHoldingRequiredTool(p)) {
                    stop(false);
                    return;
                }

                ProspectingBlock latestBlock = dataManager.getBlock(block.getId());
                if (latestBlock == null || latestBlock.isOpened()) {
                    stop(false);
                    return;
                }

                Block target = p.getTargetBlockExact(6);
                if (target == null || !isSameBlock(target, latestBlock.getLocation())) {
                    stop(false);
                    return;
                }

                spawnBrushParticle(target, latestBlock.getSkin());
                if (step % 4 == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_BRUSH_BRUSHING_GENERIC, 0.8f, 1.0f);
                }

                step++;
                if (step >= needSteps) {
                    finishProspecting(p, latestBlock, target);
                    stop(true);
                }
            }

            private void stop(boolean completed) {
                BukkitTask running = brushingTasks.remove(playerId);
                if (running != null) {
                    running.cancel();
                }
                blockBusyBy.remove(key, playerId);
                if (!completed) {
                    Block refreshed = block.getLocation().getBlock();
                    spawnBrushParticle(refreshed, block.getSkin());
                }
            }
        }, 0L, period);

        brushingTasks.put(playerId, task);
    }

    private void finishProspecting(Player player, ProspectingBlock block, Block clicked) {
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

        clicked.getWorld().playSound(clicked.getLocation(), Sound.BLOCK_SUSPICIOUS_SAND_BREAK, 1.0f, 1.2f);
        block.setOpened(true);
        block.setOpenedAtMillis(System.currentTimeMillis());
        dataManager.saveBlock(block);
    }

    private void spawnBrushParticle(Block block, Material skin) {
        Color color = skin == Material.SUSPICIOUS_SAND ? Color.fromRGB(245, 230, 170) : Color.fromRGB(255, 255, 255);
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.1f);
        block.getWorld().spawnParticle(Particle.DUST, block.getLocation().add(0.5, 0.9, 0.5), 8, 0.2, 0.15, 0.2, 0.0, dust);
    }

    private boolean isHoldingRequiredTool(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        Material requiredTool;
        try {
            requiredTool = Material.valueOf(plugin.getConfig().getString("defaults.required-tool", "BRUSH").toUpperCase());
        } catch (IllegalArgumentException ex) {
            requiredTool = Material.BRUSH;
        }
        return hand.getType() == requiredTool;
    }

    private boolean isSameBlock(Block block, org.bukkit.Location location) {
        if (location.getWorld() == null || block.getWorld() == null) {
            return false;
        }
        return block.getWorld().getUID().equals(location.getWorld().getUID())
                && block.getX() == location.getBlockX()
                && block.getY() == location.getBlockY()
                && block.getZ() == location.getBlockZ();
    }

    private String blockKey(org.bukkit.Location location) {
        if (location.getWorld() == null) {
            return "null:0:0:0";
        }
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
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
