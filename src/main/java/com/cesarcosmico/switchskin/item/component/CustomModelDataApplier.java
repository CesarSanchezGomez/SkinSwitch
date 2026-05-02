package com.cesarcosmico.switchskin.item.component;

import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.List;

public final class CustomModelDataApplier extends BaseComponentApplier {

    @Override
    public String key() {
        return "custom_model_data";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final ConfigurationSection cmdSection = section.getConfigurationSection(key());
        if (cmdSection == null) return;
        applyTo(item, parse(cmdSection));
    }

    public static CustomModelDataConfig parse(ConfigurationSection section) {
        return new CustomModelDataConfig(
                section.getFloatList("floats"),
                section.getBooleanList("flags"),
                section.getStringList("strings"),
                section.getIntegerList("colors"));
    }

    public static void applyTo(ItemStack item, CustomModelDataConfig config) {
        if (config == null || config.isEmpty()) return;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        final CustomModelDataComponent cmd = meta.getCustomModelDataComponent();

        if (!config.floats().isEmpty()) cmd.setFloats(config.floats());
        if (!config.flags().isEmpty()) cmd.setFlags(config.flags());
        if (!config.strings().isEmpty()) cmd.setStrings(config.strings());
        if (!config.colors().isEmpty()) {
            final List<Color> colors = config.colors().stream().map(Color::fromRGB).toList();
            cmd.setColors(colors);
        }

        meta.setCustomModelDataComponent(cmd);
        item.setItemMeta(meta);
    }
}
