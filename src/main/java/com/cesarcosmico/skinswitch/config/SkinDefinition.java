package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;

import java.util.List;

/**
 * A skin definition loaded from skins.yml.
 *
 * Only {@code id} and {@code itemModel} are required. Other fields are
 * optional; when null/empty the service falls back to the item's
 * captured original values (or the skin id, for {@code icon}).
 *
 * Icons can be specialised per state via {@code iconActive} and
 * {@code iconInactive}. Either one falls back to {@code icon} when not
 * defined, so simple skins can keep using a single {@code icon} field.
 *
 * Bracket colours have two slots: {@code bracketColor} when the slot's
 * tooltip token has been applied, {@code bracketColorDefault} when it
 * hasn't. Both fall back to the global {@code default-bracket-color}.
 */
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
