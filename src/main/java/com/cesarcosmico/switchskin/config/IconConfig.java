package com.cesarcosmico.switchskin.config;

import org.bukkit.NamespacedKey;

import java.util.List;

public record IconConfig(
        String material,
        String customName,
        List<String> lore,
        NamespacedKey itemModel
) {
    public IconConfig {
        lore = lore == null ? List.of() : List.copyOf(lore);
    }
}
