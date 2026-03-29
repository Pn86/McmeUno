package cn.pn86.pnextremesurvival.service;

import cn.pn86.pnextremesurvival.PnExtremeSurvivalPlugin;
import cn.pn86.pnextremesurvival.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LootChestService {

    private final PnExtremeSurvivalPlugin plugin;

    public LootChestService(PnExtremeSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("features.death-loot-chest", true);
    }

    public void createLootChest(Player player, Location deathLocation, List<ItemStack> drops) {
        if (!isEnabled() || drops.isEmpty()) {
            return;
        }

        List<ItemStack> cleanDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop != null && !drop.getType().isAir()) {
                cleanDrops.add(drop.clone());
            }
        }
        if (cleanDrops.isEmpty()) {
            return;
        }

        Location chestLocation = findBestLocation(deathLocation);
        if (chestLocation == null) {
            return;
        }

        Block block = chestLocation.getBlock();
        block.setType(Material.CHEST, false);

        if (!(block.getState() instanceof Chest chest)) {
            return;
        }

        for (ItemStack item : cleanDrops) {
            chest.getBlockInventory().addItem(item).values()
                    .forEach(leftover -> block.getWorld().dropItemNaturally(chestLocation, leftover));
        }

        String msg = MessageUtil.format(plugin, "loot-chest-created")
                .replace("%world%", chestLocation.getWorld().getName())
                .replace("%x%", String.valueOf(chestLocation.getBlockX()))
                .replace("%y%", String.valueOf(chestLocation.getBlockY()))
                .replace("%z%", String.valueOf(chestLocation.getBlockZ()));
        MessageUtil.sendRaw(player, msg);
    }

    private Location findBestLocation(Location deathLocation) {
        World world = deathLocation.getWorld();
        if (world == null) {
            return null;
        }

        if (canPlaceAt(deathLocation.getBlock())) {
            return deathLocation.getBlock().getLocation();
        }

        int radius = Math.max(1, plugin.getConfig().getInt("death-loot.search-radius", 5));
        int baseX = deathLocation.getBlockX();
        int baseY = deathLocation.getBlockY();
        int baseZ = deathLocation.getBlockZ();

        for (int r = 1; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -1; y <= 1; y++) {
                        Block candidate = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                        if (canPlaceAt(candidate)) {
                            return candidate.getLocation();
                        }
                    }
                }
            }
        }

        int highestY = world.getHighestBlockYAt(baseX, baseZ);
        Block top = world.getBlockAt(baseX, highestY + 1, baseZ);
        if (canPlaceAt(top)) {
            return top.getLocation();
        }

        Block replace = world.getBlockAt(baseX, highestY, baseZ);
        return replace.getLocation();
    }

    private boolean canPlaceAt(Block block) {
        Block above = block.getRelative(0, 1, 0);
        Block below = block.getRelative(0, -1, 0);

        boolean blockPassable = block.isPassable() || block.getType().isAir();
        boolean abovePassable = above.isPassable() || above.getType().isAir();
        boolean belowSolid = below.getType().isSolid();

        return blockPassable && abovePassable && belowSolid;
    }
}
