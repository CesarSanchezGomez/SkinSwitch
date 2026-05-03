package com.cesarcosmico.switchskin.config;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

public record TokenVisualConfig(
        @Nullable NamespacedKey itemModel,
        @Nullable CustomModelDataConfig customModelData
) {
    public boolean isEmpty() {
        return itemModel == null && customModelData == null;
    }
}
