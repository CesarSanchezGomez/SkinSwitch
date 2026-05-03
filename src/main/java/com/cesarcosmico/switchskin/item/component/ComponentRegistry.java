package com.cesarcosmico.switchskin.item.component;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class ComponentRegistry {

    private final Map<String, ComponentApplier> appliers;

    public ComponentRegistry(Logger logger) {
        this.appliers = new LinkedHashMap<>();

        register(new ProfileApplier(logger));
        register(new CustomNameApplier());
        register(new LoreApplier());
        register(new EnchantmentsApplier(logger));
        register(new CustomModelDataApplier());
        register(new ItemModelApplier(logger));
        register(new PotionContentsApplier());
        register(new TooltipDisplayApplier(logger));
        register(new TooltipStyleApplier(logger));
        register(new EnchantmentGlintOverrideApplier());
    }

    private void register(ComponentApplier applier) {
        appliers.put(applier.key(), applier);
    }

    public void applyAll(ItemStack item, ConfigurationSection section) {
        for (ComponentApplier applier : appliers.values()) {
            if (section.contains(applier.key())) {
                applier.apply(item, section);
            }
        }
    }
}
