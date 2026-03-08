package cn.pn86.pnwarp.gui;

import cn.pn86.pnwarp.model.Warp;
import cn.pn86.pnwarp.service.WarpService;
import cn.pn86.pnwarp.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WarpGuiManager {
    private final JavaPlugin plugin;
    private final WarpService warpService;
    private FileConfiguration guiConfig;
    private final Map<UUID, Integer> pages = new HashMap<>();

    public WarpGuiManager(JavaPlugin plugin, WarpService warpService) {
        this.plugin = plugin;
        this.warpService = warpService;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        if (!file.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player, int page) {
        int rows = 6;
        int size = rows * 9;
        String title = TextUtil.color(guiConfig.getString("menu.title", "&8地标菜单"));
        Inventory inv = Bukkit.createInventory(player, size, title);

        List<Warp> warps = warpService.allWarpsSorted();
        int warpsPerPage = 45;
        int maxPage = Math.max(1, (int) Math.ceil(warps.size() / (double) warpsPerPage));
        int safePage = Math.min(Math.max(page, 1), maxPage);
        pages.put(player.getUniqueId(), safePage);

        int start = (safePage - 1) * warpsPerPage;
        int end = Math.min(start + warpsPerPage, warps.size());
        int slot = 0;
        for (int i = start; i < end; i++) {
            inv.setItem(slot++, warpItem(warps.get(i)));
        }

        ItemStack filler = createConfiguredItem("menu.bottom.filler", Material.GRAY_STAINED_GLASS_PANE,
                " ", List.of());
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        int prevSlot = guiConfig.getInt("menu.bottom.previous.slot", 45);
        int nextSlot = guiConfig.getInt("menu.bottom.next.slot", 53);
        int closeSlot = guiConfig.getInt("menu.bottom.close.slot", 49);

        if (safePage > 1) {
            inv.setItem(prevSlot, createConfiguredItem("menu.bottom.previous", Material.ARROW,
                    "&e上一页", List.of("&7点击查看上一页")));
        }
        if (safePage < maxPage) {
            inv.setItem(nextSlot, createConfiguredItem("menu.bottom.next", Material.ARROW,
                    "&e下一页", List.of("&7点击查看下一页")));
        }

        inv.setItem(closeSlot, createConfiguredItem("menu.bottom.close", Material.BARRIER,
                "&c关闭", List.of("&7点击关闭菜单")));

        player.openInventory(inv);
    }

    private ItemStack warpItem(Warp warp) {
        ItemStack item = new ItemStack(warp.icon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = guiConfig.getString("menu.warp-item.name", "&a{warp}")
                    .replace("{warp}", warp.name());
            List<String> loreTemplate = guiConfig.getStringList("menu.warp-item.lore");
            List<String> lore = new ArrayList<>();
            for (String line : loreTemplate) {
                lore.add(line
                        .replace("{description}", warp.description())
                        .replace("{owner}", warp.ownerName())
                        .replace("{world}", warp.location().getWorld().getName())
                        .replace("{x}", String.valueOf(warp.location().getBlockX()))
                        .replace("{y}", String.valueOf(warp.location().getBlockY()))
                        .replace("{z}", String.valueOf(warp.location().getBlockZ())));
            }
            meta.setDisplayName(TextUtil.color(name));
            meta.setItemName(warp.name());
            meta.setLore(TextUtil.color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openNext(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), 1) + 1);
    }

    public void openPrev(Player player) {
        open(player, pages.getOrDefault(player.getUniqueId(), 1) - 1);
    }

    public int getCurrentPage(Player player) {
        return pages.getOrDefault(player.getUniqueId(), 1);
    }

    public String title() {
        return TextUtil.color(guiConfig.getString("menu.title", "&8地标菜单"));
    }

    public int previousSlot() {
        return guiConfig.getInt("menu.bottom.previous.slot", 45);
    }

    public int nextSlot() {
        return guiConfig.getInt("menu.bottom.next.slot", 53);
    }

    public int closeSlot() {
        return guiConfig.getInt("menu.bottom.close.slot", 49);
    }

    private ItemStack createConfiguredItem(String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore) {
        Material material = Material.matchMaterial(guiConfig.getString(path + ".material", fallbackMaterial.name()));
        if (material == null || !material.isItem()) {
            material = fallbackMaterial;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(TextUtil.color(guiConfig.getString(path + ".name", fallbackName)));
            List<String> lore = guiConfig.getStringList(path + ".lore");
            if (lore.isEmpty()) {
                lore = fallbackLore;
            }
            meta.setLore(TextUtil.color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
