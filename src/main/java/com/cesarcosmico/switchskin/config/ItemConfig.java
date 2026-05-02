package com.cesarcosmico.switchskin.config;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public record ItemConfig(
        ItemStack baseItem,
        String customNameRaw,
        List<String> loreRaw
) {
    public ItemConfig {
        loreRaw = loreRaw == null ? List.of() : List.copyOf(loreRaw);
    }

    public boolean hasDynamicName() {
        return customNameRaw != null && !customNameRaw.isEmpty();
    }

    public boolean hasDynamicLore() {
        return !loreRaw.isEmpty();
    }
}
