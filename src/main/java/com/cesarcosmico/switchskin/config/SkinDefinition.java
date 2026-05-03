package com.cesarcosmico.switchskin.config;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SkinDefinition(
        String id,
        @Nullable NamespacedKey itemModel,
        String name,
        List<String> lore,
        String iconActive,
        String iconInactive,
        String bracketColorActive,
        String bracketColorInactive,
        NamespacedKey tooltipStyle,
        @Nullable CustomModelDataConfig customModelData,
        @Nullable TooltipDisplayConfig tooltipDisplay
) {
    public SkinDefinition {
        lore = lore == null ? List.of() : List.copyOf(lore);
    }

    public String nameOrId() {
        return (name != null && !name.isEmpty()) ? name : id;
    }

    public boolean hasName() {
        return name != null && !name.isEmpty();
    }

    public boolean hasLore() {
        return !lore.isEmpty();
    }

    public boolean hasBracketColorActive() {
        return bracketColorActive != null && !bracketColorActive.isEmpty();
    }

    public boolean hasBracketColorInactive() {
        return bracketColorInactive != null && !bracketColorInactive.isEmpty();
    }

    public boolean hasIconActive() {
        return iconActive != null && !iconActive.isEmpty();
    }

    public boolean hasIconInactive() {
        return iconInactive != null && !iconInactive.isEmpty();
    }
}
