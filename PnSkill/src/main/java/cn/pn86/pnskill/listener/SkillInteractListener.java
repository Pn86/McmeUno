package cn.pn86.pnskill.listener;

import cn.pn86.pnskill.service.SkillCastService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SkillInteractListener implements Listener {
    private final SkillCastService skillCastService;

    public SkillInteractListener(SkillCastService skillCastService) {
        this.skillCastService = skillCastService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        String mode = player.isSneaking() ? "b" : "a";
        skillCastService.castFromItem(player, mode);
    }
}
