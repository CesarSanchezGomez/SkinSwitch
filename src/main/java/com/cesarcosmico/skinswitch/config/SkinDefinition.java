package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;

/**
 * A skin definition loaded from skins.yml.
 *
 * Only {@code id} and {@code itemModel} are required. The other fields
 * are optional; when null/empty the service falls back to the item's
 * captured original values (or the skin id, for {@code icon}).
 */
public record SkinDefinition(
        String id,
        NamespacedKey itemModel,
        String display,
        String icon,
        NamespacedKey tooltipStyle,
        String color
) {
    public String displayOrId() {
        return (display != null && !display.isEmpty()) ? display : id;
    }

    public boolean hasDisplay() {
        return display != null && !display.isEmpty();
    }

    public boolean hasColor() {
        return color != null && !color.isEmpty();
    }
}
