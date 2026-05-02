package com.cesarcosmico.switchskin.item.component;

import com.cesarcosmico.switchskin.config.TooltipDisplayConfig;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class TooltipDisplayApplier implements ComponentApplier {

    private static final Map<String, DataComponentType> TYPE_REGISTRY = buildTypeRegistry();

    private final Logger logger;

    public TooltipDisplayApplier(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key() {
        return "tooltip_display";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final ConfigurationSection tooltipSection = section.getConfigurationSection(key());
        if (tooltipSection == null) return;
        applyTo(item, parse(tooltipSection), logger);
    }

    public static TooltipDisplayConfig parse(ConfigurationSection section) {
        return new TooltipDisplayConfig(
                section.getBoolean("hide_tooltip", false),
                section.getStringList("hidden_components"));
    }

    public static void applyTo(ItemStack item, TooltipDisplayConfig config, Logger logger) {
        if (config == null) return;

        final TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();
        if (config.hideTooltip()) builder.hideTooltip(true);

        for (String componentId : config.hiddenComponents()) {
            final DataComponentType type = resolveType(componentId);
            if (type == null) {
                logger.warning("Unknown component for tooltip_display hidden_components: " + componentId);
                continue;
            }
            builder.addHiddenComponents(type);
        }

        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, builder.build());
    }

    public static boolean knowsComponent(String componentId) {
        return resolveType(componentId) != null;
    }

    private static DataComponentType resolveType(String componentId) {
        final String normalized = componentId.replace("minecraft:", "").toLowerCase();
        return TYPE_REGISTRY.get(normalized);
    }

    private static Map<String, DataComponentType> buildTypeRegistry() {
        final Map<String, DataComponentType> map = new HashMap<>();
        for (Field field : DataComponentTypes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())
                    || !Modifier.isPublic(field.getModifiers())
                    || !DataComponentType.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                map.put(field.getName().toLowerCase(), (DataComponentType) field.get(null));
            } catch (IllegalAccessException e) {
                System.getLogger("TooltipDisplayApplier")
                        .log(System.Logger.Level.WARNING, "Failed to access field: " + field.getName(), e);
            }
        }
        return map;
    }
}
