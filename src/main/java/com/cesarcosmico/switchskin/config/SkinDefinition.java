package com.cesarcosmico.switchskin.config;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record SkinDefinition(
        String id,
        NamespacedKey itemModel,
        String name,
        List<String> lore,
        String icon,
        String iconActive,
        String iconInactive,
        String bracketColor,
        String bracketColorDefault,
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

    public boolean hasBracketColor() {
        return bracketColor != null && !bracketColor.isEmpty();
    }

    public boolean hasBracketColorDefault() {
        return bracketColorDefault != null && !bracketColorDefault.isEmpty();
    }

    public String activeIcon() {
        return (iconActive != null && !iconActive.isEmpty()) ? iconActive : icon;
    }

    public String inactiveIcon() {
        return (iconInactive != null && !iconInactive.isEmpty()) ? iconInactive : icon;
    }
}
