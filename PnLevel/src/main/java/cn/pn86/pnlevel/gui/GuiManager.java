package cn.pn86.pnlevel.gui;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {
    private static final List<Integer> PYRAMID_SLOTS = List.of(
            4,
            12, 14,
            20, 22, 24,
            28, 30, 32, 34,
            36, 38, 40, 42, 44
    );

    private final PnLevelPlugin plugin;

    public GuiManager(PnLevelPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGift(Player player, int page) {
        int rows = Math.max(2, Math.min(6, plugin.getGuiConfig().getInt("gift.rows", 6)));
        int size = rows * 9;
        int contentSize = (rows - 1) * 9;
        int maxLevel = plugin.getMaxLevel();
        int pageCount = Math.max(1, (int) Math.ceil(maxLevel / (double) contentSize));
        int safePage = Math.max(1, Math.min(pageCount, page));

        String title = plugin.getGuiConfig().getString("gift.title", "&8等级奖励菜单")
                .replace("%page%", String.valueOf(safePage))
                .replace("%pages%", String.valueOf(pageCount));
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GIFT, safePage, pageCount), size, ColorUtil.component(title));

        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        int startLevel = (safePage - 1) * contentSize + 1;
        for (int slot = 0; slot < contentSize; slot++) {
            int level = startLevel + slot;
            if (level > maxLevel) {
                break;
            }
            inv.setItem(slot, buildGiftLevelItem(data, level));
        }

        int prevSlot = size - 9;
        int claimAllSlot = size - 5;
        int nextSlot = size - 1;
        int returnSlot = resolveReturnSlot(rows, plugin.getGuiConfig().getString("gift.return-position", "left_bottom"));

        if (safePage > 1) {
            inv.setItem(prevSlot, button(Material.ARROW, plugin.getGuiConfig().getString("gift.buttons.prev", "&e上一页")));
        }
        inv.setItem(claimAllSlot, button(Material.CHEST, plugin.getGuiConfig().getString("gift.buttons.claim-all", "&a一键领取全部")));
        if (safePage < pageCount) {
            inv.setItem(nextSlot, button(Material.ARROW, plugin.getGuiConfig().getString("gift.buttons.next", "&e下一页")));
        }
        inv.setItem(returnSlot, button(Material.BARRIER, plugin.getGuiConfig().getString("common.return-name", "&c返回")));

        player.openInventory(inv);
    }

    public void openTop(Player player) {
        int rows = 5;
        int size = rows * 9;
        String title = plugin.getGuiConfig().getString("top.title", "&8等级排行榜(金字塔)");
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.TOP, 1, 1), size, ColorUtil.component(title));
        List<PlayerLevelData> top = plugin.getLevelManager().top();
        int max = Math.min(PYRAMID_SLOTS.size(), top.size());

        for (int i = 0; i < max; i++) {
            PlayerLevelData data = top.get(i);
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(ColorUtil.component(plugin.getGuiConfig().getString("top.item.name", "&e#%rank% &f%player%")
                    .replace("%rank%", String.valueOf(i + 1))
                    .replace("%player%", data.getLastName())));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : plugin.getGuiConfig().getStringList("top.item.lore")) {
                lore.add(ColorUtil.component(line
                        .replace("%rank%", String.valueOf(i + 1))
                        .replace("%player%", data.getLastName())
                        .replace("%level%", String.valueOf(data.getLevel()))
                        .replace("%exp%", String.valueOf(data.getExp()))));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.setItem(PYRAMID_SLOTS.get(i), item);
        }

        int returnSlot = resolveReturnSlot(rows, plugin.getGuiConfig().getString("top.return-position", "middle_bottom"));
        inv.setItem(returnSlot, button(Material.BARRIER, plugin.getGuiConfig().getString("common.return-name", "&c返回")));
        player.openInventory(inv);
    }

    public void handleGiftClick(Player player, MenuHolder holder, int rawSlot) {
        int rows = Math.max(2, Math.min(6, plugin.getGuiConfig().getInt("gift.rows", 6)));
        int size = rows * 9;
        int contentSize = (rows - 1) * 9;
        int maxLevel = plugin.getMaxLevel();

        int prevSlot = size - 9;
        int claimAllSlot = size - 5;
        int nextSlot = size - 1;
        int returnSlot = resolveReturnSlot(rows, plugin.getGuiConfig().getString("gift.return-position", "left_bottom"));

        if (rawSlot == prevSlot && holder.page() > 1) {
            openGift(player, holder.page() - 1);
            return;
        }
        if (rawSlot == nextSlot && holder.page() < holder.pageCount()) {
            openGift(player, holder.page() + 1);
            return;
        }
        if (rawSlot == claimAllSlot) {
            claimAll(player);
            openGift(player, holder.page());
            return;
        }
        if (rawSlot == returnSlot) {
            runReturnCommand(player);
            player.closeInventory();
            return;
        }

        if (rawSlot >= 0 && rawSlot < contentSize) {
            int level = (holder.page() - 1) * contentSize + rawSlot + 1;
            if (level <= maxLevel) {
                claimSingle(player, level);
                openGift(player, holder.page());
            }
        }
    }

    public void handleTopClick(Player player, int rawSlot) {
        int returnSlot = resolveReturnSlot(5, plugin.getGuiConfig().getString("top.return-position", "middle_bottom"));
        if (rawSlot == returnSlot) {
            runReturnCommand(player);
            player.closeInventory();
        }
    }

    private void claimSingle(Player player, int level) {
        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        if (plugin.getGiftManager().isClaimed(data, level)) {
            player.sendMessage(ColorUtil.component(plugin.msg("gift-already-claimed")));
            return;
        }
        if (!plugin.getGiftManager().isClaimable(data, level)) {
            player.sendMessage(ColorUtil.component(plugin.msg("gift-locked").replace("%level%", String.valueOf(level))));
            return;
        }
        plugin.getGiftManager().claimLevel(player, data, level);
        player.sendMessage(ColorUtil.component(plugin.msg("gift-level-claimed").replace("%level%", String.valueOf(level))));
    }

    private void claimAll(Player player) {
        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        int claimed = plugin.getGiftManager().claimAll(player, data);
        if (claimed <= 0) {
            player.sendMessage(ColorUtil.component(plugin.msg("gift-none")));
            return;
        }
        player.sendMessage(ColorUtil.component(plugin.msg("gift-claimed").replace("%count%", String.valueOf(claimed))));
    }

    private ItemStack buildGiftLevelItem(PlayerLevelData data, int level) {
        String path;
        if (plugin.getGiftManager().isClaimed(data, level)) {
            path = "gift.level-item.claimed";
        } else if (plugin.getGiftManager().isClaimable(data, level)) {
            path = "gift.level-item.claimable";
        } else {
            path = "gift.level-item.locked";
        }

        Material material = Material.matchMaterial(plugin.getGuiConfig().getString(path + ".material", "PAPER"));
        if (material == null) material = Material.PAPER;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(ColorUtil.component(replace(plugin.getGuiConfig().getString(path + ".name", "&e等级 %level%"), data, level)));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : plugin.getGuiConfig().getStringList(path + ".lore")) {
            lore.add(ColorUtil.component(replace(line, data, level)));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private String replace(String text, PlayerLevelData data, int level) {
        return (text == null ? "" : text)
                .replace("%level%", String.valueOf(level))
                .replace("%player_level%", String.valueOf(data.getLevel()))
                .replace("%player_exp%", String.valueOf(data.getExp()))
                .replace("%last_claimed%", String.valueOf(data.getLastClaimedLevel()));
    }

    private ItemStack button(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(ColorUtil.component(name));
        stack.setItemMeta(meta);
        return stack;
    }

    private int resolveReturnSlot(int rows, String position) {
        int size = rows * 9;
        int bottomStart = size - 9;
        if ("middle_bottom".equalsIgnoreCase(position)) {
            return bottomStart + 4;
        }
        return bottomStart;
    }

    private void runReturnCommand(Player player) {
        String command = plugin.getConfig().getString("menu.return-command", "spawn");
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            player.performCommand(command);
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }

    public enum MenuType {
        GIFT,
        TOP
    }

    public record MenuHolder(MenuType type, int page, int pageCount) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
