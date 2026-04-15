package cn.pn86.pnskill.listener;

import cn.pn86.pnskill.service.SkillCastService;
import cn.pn86.pnskill.service.SkillCastService.CastResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SkillTriggerListener implements Listener {
    private final SkillCastService skillCastService;

    public SkillTriggerListener(SkillCastService skillCastService) {
        this.skillCastService = skillCastService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack triggerItem = event.getItem();
        String mode = player.isSneaking() ? "b" : "a";
        CastResult result = skillCastService.castFromItem(player, triggerItem, mode);
        if (result == CastResult.CAST_SUCCESS || result == CastResult.ON_COOLDOWN) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack triggerItem = hand == EquipmentSlot.HAND
                ? player.getInventory().getItemInMainHand()
                : player.getInventory().getItemInOffHand();
        String mode = player.isSneaking() ? "b" : "a";

        CastResult result = skillCastService.castFromItem(player, triggerItem, mode);
        if (result == CastResult.CAST_SUCCESS || result == CastResult.ON_COOLDOWN) {
            event.setCancelled(true);
        }
    }
}
