package com.pn86.pngoldprospecting;

import org.bukkit.inventory.ItemStack;

public record LootEntry(String itemId, ItemStack itemStack, int weight, String command) {
    public boolean isCommandLoot() {
        return command != null && !command.isBlank();
    }
}
