package com.cesarcosmico.switchskin.item.component;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

public final class ItemModelApplier extends BaseComponentApplier {

    private final Logger logger;

    public ItemModelApplier(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key() {
        return "item_model";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final String value = section.getString(key());
        if (value == null || value.isEmpty()) return;

        try {
            final NamespacedKey modelKey = NamespacedKey.fromString(value);
            if (modelKey == null) {
                logger.warning("Invalid item_model key: " + value);
                return;
            }
            editMeta(item, meta -> meta.setItemModel(modelKey));
        } catch (Exception e) {
            logger.warning("Failed to apply item_model '" + value + "': " + e.getMessage());
        }
    }
}
