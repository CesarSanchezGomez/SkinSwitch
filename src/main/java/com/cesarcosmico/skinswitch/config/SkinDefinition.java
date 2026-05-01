package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;

import java.util.List;

/**
 * A skin definition loaded from skins.yml.
 *
 * Only {@code id} and {@code itemModel} are required. Other fields are
 * optional; when null/empty the service falls back to the item's
 * captured original values (or the skin id, for {@code icon}).
 */
public record SkinDefinition(
        String id,
        NamespacedKey itemModel,
        String name,
        List<String> lore,
        String icon,
        String color,
        NamespacedKey tooltipStyle
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

    public boolean hasColor() {
        return color != null && !color.isEmpty();
    }
}
