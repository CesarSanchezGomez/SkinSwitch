package com.cesarcosmico.switchskin.item.component;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public interface ComponentApplier {

    String key();

    void apply(ItemStack item, ConfigurationSection section);
}
