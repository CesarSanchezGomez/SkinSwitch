package com.cesarcosmico.switchskin.item.component;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

public final class EnchantmentsApplier extends BaseComponentApplier {

    private final Logger logger;

    public EnchantmentsApplier(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key() {
        return "enchantments";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final ConfigurationSection enchSection = section.getConfigurationSection(key());
        if (enchSection == null) return;

        editMeta(item, meta -> {
            for (String enchantKey : enchSection.getKeys(false)) {
                final int level = enchSection.getInt(enchantKey, 1);
                final NamespacedKey nsKey = NamespacedKey.minecraft(enchantKey.toLowerCase());
                final Enchantment enchantment = RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.ENCHANTMENT)
                        .get(nsKey);
                if (enchantment == null) {
                    logger.warning("Unknown enchantment: " + enchantKey);
                    continue;
                }
                meta.addEnchant(enchantment, level, true);
            }
        });
    }
}
