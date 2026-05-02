package com.cesarcosmico.switchskin.item.component;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class EnchantmentGlintOverrideApplier extends BaseComponentApplier {

    @Override
    public String key() {
        return "enchantment_glint_override";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        if (!section.contains(key())) return;
        editMeta(item, meta -> meta.setEnchantmentGlintOverride(section.getBoolean(key())));
    }
}
