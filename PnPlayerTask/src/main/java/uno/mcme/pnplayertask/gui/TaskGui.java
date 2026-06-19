package uno.mcme.pnplayertask.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import uno.mcme.pnplayertask.PnPlayerTaskPlugin;
import uno.mcme.pnplayertask.task.PlayerTask;
import uno.mcme.pnplayertask.util.Text;

import java.io.File;
import java.util.*;

public class TaskGui {
    private final PnPlayerTaskPlugin plugin;
    private final NamespacedKey key;
    public TaskGui(PnPlayerTaskPlugin plugin) { this.plugin = plugin; this.key = new NamespacedKey(plugin, "task_id"); }
    public void open(Player player, int page) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
        int size = Math.max(9, Math.min(54, cfg.getInt("size", 54) / 9 * 9));
        Inventory inv = Bukkit.createInventory(null, size, Text.color(Text.papi(player, cfg.getString("title", "&6任务页面"))));
        if (cfg.getBoolean("buttons.filler.enabled", true)) fill(inv, item(cfg.getString("buttons.filler.item"), cfg.getString("buttons.filler.name"), cfg.getStringList("buttons.filler.lore"), player));
        List<Integer> slots = cfg.getIntegerList("task-slots"); if (slots.isEmpty()) slots = List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34);
        List<PlayerTask> tasks = new ArrayList<>(plugin.getTaskManager().getTasks());
        int start = Math.max(0, page) * slots.size();
        for (int i = 0; i < slots.size() && start + i < tasks.size(); i++) inv.setItem(slots.get(i), taskItem(player, tasks.get(start + i), cfg));
        setButton(inv, cfg, "previous", page); setButton(inv, cfg, "next", page); setButton(inv, cfg, "close", page);
        player.openInventory(inv);
    }
    private void fill(Inventory inv, ItemStack stack) { for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, stack); }
    private void setButton(Inventory inv, YamlConfiguration cfg, String id, int page) { int slot = cfg.getInt("buttons." + id + ".slot", -1); if (slot >= 0 && slot < inv.getSize()) { ItemStack it = item(cfg.getString("buttons." + id + ".item"), cfg.getString("buttons." + id + ".name"), cfg.getStringList("buttons." + id + ".lore"), null); ItemMeta meta = it.getItemMeta(); meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "__" + id + ":" + page); it.setItemMeta(meta); inv.setItem(slot, it); } }
    private ItemStack taskItem(Player player, PlayerTask task, YamlConfiguration cfg) {
        String state = plugin.getTaskManager().isClaimed(player.getUniqueId(), task.id()) ? "claimed" : (plugin.getTaskManager().isComplete(player, task.id()) ? "complete" : "incomplete");
        Material mat = Material.matchMaterial(cfg.getString("task-display." + state + ".item", task.item().name())); if (mat == null) mat = task.item();
        List<String> lore = new ArrayList<>(task.lore()); lore.addAll(cfg.getStringList("task-display." + state + ".lore-add"));
        ItemStack it = item(mat.name(), task.name(), lore, player); ItemMeta meta = it.getItemMeta(); meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, task.id()); it.setItemMeta(meta); return it;
    }
    private ItemStack item(String material, String name, List<String> lore, Player player) { Material mat = Material.matchMaterial(material == null ? "paper" : material); if (mat == null) mat = Material.PAPER; ItemStack it = new ItemStack(mat); ItemMeta meta = it.getItemMeta(); meta.setDisplayName(Text.color(Text.papi(player, name == null ? "" : name))); meta.setLore(Text.color(Text.papi(player, lore))); it.setItemMeta(meta); return it; }
    public NamespacedKey key() { return key; }
}
