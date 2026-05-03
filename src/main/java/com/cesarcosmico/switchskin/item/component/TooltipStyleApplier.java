package com.cesarcosmico.switchskin.item.component;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

public final class TooltipStyleApplier extends BaseComponentApplier {

    private final Logger logger;

    public TooltipStyleApplier(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key() {
        return "tooltip_style";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final String value = section.getString(key());
        if (value == null || value.isEmpty()) return;

        try {
            final NamespacedKey styleKey = NamespacedKey.fromString(value);
            if (styleKey == null) {
                logger.warning("Invalid tooltip_style key: " + value);
                return;
            }
            editMeta(item, meta -> meta.setTooltipStyle(styleKey));
        } catch (Exception e) {
            logger.warning("Failed to apply tooltip_style '" + value + "': " + e.getMessage());
        }
    }
}
