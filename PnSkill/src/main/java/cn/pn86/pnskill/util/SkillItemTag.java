package cn.pn86.pnskill.util;

import cn.pn86.pnskill.PnSkillPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SkillItemTag {
    private final NamespacedKey key;

    public SkillItemTag(PnSkillPlugin plugin) {
        this.key = new NamespacedKey(plugin, "bound_skill");
    }

    public String getBoundSkill(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(key, PersistentDataType.STRING);
    }

    public boolean bindSkill(ItemStack item, String skillId) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skillId);
        item.setItemMeta(meta);
        return true;
    }
}
