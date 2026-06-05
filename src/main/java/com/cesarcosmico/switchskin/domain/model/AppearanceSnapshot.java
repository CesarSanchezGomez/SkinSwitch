package com.cesarcosmico.switchskin.domain.model;

import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import com.cesarcosmico.switchskin.config.TooltipDisplayConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Bukkit-free serialized form of an item's original appearance (gson JSON /
// NamespacedKey strings), captured on the first slot and restored on the last.
public record AppearanceSnapshot(
        boolean present,
        List<String> loreJson,
        @Nullable String nameJson,
        @Nullable String itemModel,
        @Nullable String tooltipStyle,
        @Nullable CustomModelDataConfig customModelData,
        @Nullable TooltipDisplayConfig tooltipDisplay
) {
    public AppearanceSnapshot {
        loreJson = loreJson == null ? List.of() : List.copyOf(loreJson);
    }

    public static AppearanceSnapshot absent() {
        return new AppearanceSnapshot(false, List.of(), null, null, null, null, null);
    }
}
