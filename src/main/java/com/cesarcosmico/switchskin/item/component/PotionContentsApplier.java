package com.cesarcosmico.switchskin.item.component;

import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public final class PotionContentsApplier implements ComponentApplier {

    @Override
    public String key() {
        return "potion_contents";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final ConfigurationSection potionSection = section.getConfigurationSection(key());
        if (potionSection == null) return;
        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) return;

        if (potionSection.contains("custom_color")) {
            potionMeta.setColor(Color.fromRGB(potionSection.getInt("custom_color")));
        }

        item.setItemMeta(potionMeta);
    }
}
