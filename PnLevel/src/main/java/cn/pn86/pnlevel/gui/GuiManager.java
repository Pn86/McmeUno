package cn.pn86.pnlevel.gui;

import cn.pn86.pnlevel.PnLevelPlugin;
import cn.pn86.pnlevel.data.PlayerLevelData;
import cn.pn86.pnlevel.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {
    private static final List<Integer> DEFAULT_PYRAMID_SLOTS = List.of(
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
        int contentSize = Math.max(1, plugin.getGuiConfig().getIntegerList("gift.content-slots").size());
        List<Integer> contentSlots = plugin.getGuiConfig().getIntegerList("gift.content-slots");
        if (contentSlots.isEmpty()) {
            contentSlots = new ArrayList<>();
            for (int i = 0; i < (rows - 1) * 9; i++) contentSlots.add(i);
            contentSize = contentSlots.size();
        }

        int maxLevel = plugin.getMaxLevel();
        int pageCount = Math.max(1, (int) Math.ceil(maxLevel / (double) contentSize));
        int safePage = Math.max(1, Math.min(pageCount, page));

        String title = plugin.getGuiConfig().getString("gift.title", "&8等级奖励菜单")
                .replace("%page%", String.valueOf(safePage))
                .replace("%pages%", String.valueOf(pageCount));
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.GIFT, safePage, pageCount), size, ColorUtil.component(title));

        PlayerLevelData data = plugin.getPlayerDataManager().getOrCreate(player.getUniqueId(), player.getName());
        int startIndex = (safePage - 1) * contentSize;
        for (int i = 0; i < contentSlots.size(); i++) {
            int level = startIndex + i + 1;
            if (level > maxLevel) break;
            int slot = contentSlots.get(i);
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, buildGiftLevelItem(data, level));
            }
        }

        int prevSlot = plugin.getGuiConfig().getInt("gift.buttons.prev.slot", size - 6);
        int nextSlot = plugin.getGuiConfig().getInt("gift.buttons.next.slot", size - 5);
        int claimAllSlot = plugin.getGuiConfig().getInt("gift.buttons.claim-all.slot", size - 4);
        int returnSlot = plugin.getGuiConfig().getInt("gift.buttons.return.slot", size - 1);

        if (safePage > 1 && isValidSlot(prevSlot, size)) inv.setItem(prevSlot, buildConfiguredButton("gift.buttons.prev", "&e上一页", Material.ARROW));
        if (safePage < pageCount && isValidSlot(nextSlot, size)) inv.setItem(nextSlot, buildConfiguredButton("gift.buttons.next", "&e下一页", Material.ARROW));
        if (isValidSlot(claimAllSlot, size)) inv.setItem(claimAllSlot, buildConfiguredButton("gift.buttons.claim-all", "&a一键领取", Material.CHEST));
        if (isValidSlot(returnSlot, size)) inv.setItem(returnSlot, buildConfiguredButton("gift.buttons.return", plugin.getGuiConfig().getString("common.return-name", "&c返回"), Material.BARRIER));

        player.openInventory(inv);
    }

    public void openTop(Player player) {
        int rows = Math.max(1, Math.min(6, plugin.getGuiConfig().getInt("top.rows", 5)));
        int size = rows * 9;
        String title = plugin.getGuiConfig().getString("top.title", "&8等级排行榜(金字塔)");
        Inventory inv = Bukkit.createInventory(new MenuHolder(MenuType.TOP, 1, 1), size, ColorUtil.component(title));

        List<Integer> slots = plugin.getGuiConfig().getIntegerList("top.layout-slots");
        if (slots.isEmpty()) {
            slots = DEFAULT_PYRAMID_SLOTS;
        }

        List<PlayerLevelData> top = plugin.getLevelManager().top();
        int max = Math.min(Math.min(slots.size(), 15), top.size());
        for (int i = 0; i < max; i++) {
            int slot = slots.get(i);
            if (slot < 0 || slot >= size) continue;
            inv.setItem(slot, buildTopItem(top.get(i), i + 1));
        }

        int returnSlot = plugin.getGuiConfig().getInt("top.buttons.return.slot", size - 5);
        if (isValidSlot(returnSlot, size)) {
            inv.setItem(returnSlot, buildConfiguredButton("top.buttons.return", plugin.getGuiConfig().getString("common.return-name", "&c返回"), Material.BARRIER));
        }
        player.openInventory(inv);
    }

    public void handleGiftClick(Player player, MenuHolder holder, int rawSlot) {
        int rows = Math.max(2, Math.min(6, plugin.getGuiConfig().getInt("gift.rows", 6)));
        int size = rows * 9;
        int maxLevel = plugin.getMaxLevel();

        List<Integer> contentSlots = plugin.getGuiConfig().getIntegerList("gift.content-slots");
        if (contentSlots.isEmpty()) {
            contentSlots = new ArrayList<>();
            for (int i = 0; i < (rows - 1) * 9; i++) contentSlots.add(i);
        }
        int contentSize = contentSlots.size();

        int prevSlot = plugin.getGuiConfig().getInt("gift.buttons.prev.slot", size - 6);
        int nextSlot = plugin.getGuiConfig().getInt("gift.buttons.next.slot", size - 5);
        int claimAllSlot = plugin.getGuiConfig().getInt("gift.buttons.claim-all.slot", size - 4);
        int returnSlot = plugin.getGuiConfig().getInt("gift.buttons.return.slot", size - 1);

        if (isValidSlot(prevSlot, size) && rawSlot == prevSlot && holder.page() > 1) {
            openGift(player, holder.page() - 1);
            return;
        }
        if (isValidSlot(nextSlot, size) && rawSlot == nextSlot && holder.page() < holder.pageCount()) {
            openGift(player, holder.page() + 1);
            return;
        }
        if (isValidSlot(claimAllSlot, size) && rawSlot == claimAllSlot) {
            claimAll(player);
            openGift(player, holder.page());
            return;
        }
        if (isValidSlot(returnSlot, size) && rawSlot == returnSlot) {
            runReturnCommand(player);
            player.closeInventory();
            return;
        }

        int localIndex = contentSlots.indexOf(rawSlot);
        if (localIndex >= 0) {
            int level = (holder.page() - 1) * contentSize + localIndex + 1;
            if (level <= maxLevel) {
                claimSingle(player, level);
                openGift(player, holder.page());
            }
        }
    }

    public void handleTopClick(Player player, int rawSlot) {
        int rows = Math.max(1, Math.min(6, plugin.getGuiConfig().getInt("top.rows", 5)));
        int size = rows * 9;
        int returnSlot = plugin.getGuiConfig().getInt("top.buttons.return.slot", size - 5);
        if (isValidSlot(returnSlot, size) && rawSlot == returnSlot) {
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
        if (plugin.getGiftManager().isClaimed(data, level)) path = "gift.level-item.claimed";
        else if (plugin.getGiftManager().isClaimable(data, level)) path = "gift.level-item.claimable";
        else path = "gift.level-item.locked";

        return buildConfiguredItem(path, Material.PAPER,
                replace(plugin.getGuiConfig().getString(path + ".name", "&e等级 %level%"), data, level),
                plugin.getGuiConfig().getStringList(path + ".lore"), data, level);
    }

    private ItemStack buildTopItem(PlayerLevelData data, int rank) {
        String path = "top.item";
        ItemStack stack = buildConfiguredItem(path, Material.PLAYER_HEAD,
                plugin.getGuiConfig().getString(path + ".name", "&6#%rank% &f%player%")
                        .replace("%rank%", String.valueOf(rank))
                        .replace("%player%", data.getLastName()),
                plugin.getGuiConfig().getStringList(path + ".lore")
                        .stream().map(line -> line
                                .replace("%rank%", String.valueOf(rank))
                                .replace("%player%", data.getLastName())
                                .replace("%level%", String.valueOf(data.getLevel()))
                                .replace("%exp%", String.valueOf(data.getExp()))).toList(),
                data, data.getLevel());

        if (stack.getItemMeta() instanceof SkullMeta skullMeta) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(data.getUuid());
            skullMeta.setOwningPlayer(off);
            stack.setItemMeta(skullMeta);
        }
        return stack;
    }

    private ItemStack buildConfiguredButton(String path, String fallbackName, Material fallbackMaterial) {
        String name = plugin.getGuiConfig().getString(path + ".name", fallbackName);
        return buildConfiguredItem(path, fallbackMaterial, name, plugin.getGuiConfig().getStringList(path + ".lore"), null, 0);
    }

    private ItemStack buildConfiguredItem(String path, Material fallbackMaterial, String displayName, List<String> loreLines,
                                          PlayerLevelData data, int level) {
        Material material = Material.matchMaterial(plugin.getGuiConfig().getString(path + ".material", fallbackMaterial.name()));
        if (material == null) material = fallbackMaterial;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(ColorUtil.component(replace(displayName, data, level)));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ColorUtil.component(replace(line, data, level)));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private String replace(String text, PlayerLevelData data, int level) {
        String safe = text == null ? "" : text;
        if (data == null) {
            return safe.replace("%level%", String.valueOf(level));
        }
        return safe
                .replace("%level%", String.valueOf(level))
                .replace("%player_level%", String.valueOf(data.getLevel()))
                .replace("%player_exp%", String.valueOf(data.getExp()))
                .replace("%last_claimed%", String.valueOf(data.getLastClaimedLevel()));
    }

    private boolean isValidSlot(int slot, int size) {
        return slot >= 0 && slot < size;
    }

    private void runReturnCommand(Player player) {
        String command = plugin.getConfig().getString("menu.return-command", "spawn");
        boolean wasOp = player.isOp();
        try {
            if (!wasOp) player.setOp(true);
            player.performCommand(command);
        } finally {
            if (!wasOp) player.setOp(false);
        }
    }

    public enum MenuType { GIFT, TOP }

    public record MenuHolder(MenuType type, int page, int pageCount) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
