package com.cesarcosmico.switchskin.items;

import com.cesarcosmico.switchskin.config.TooltipDisplayConfig;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Bukkit boundary for the live {@code tooltip_display} data component. The skin
 * slot applies the active skin's tooltip_display directly to the held stack (the
 * RTAG dispatch covers menu icons / tokens), so this keeps that one Paper API
 * call plus the reflective component-id registry used to validate config.
 */
public final class TooltipDisplaySupport {

    private static final Map<String, DataComponentType> TYPE_REGISTRY = buildTypeRegistry();

    private TooltipDisplaySupport() {}

    public static void applyTo(ItemStack item, TooltipDisplayConfig config) {
        if (config == null) return;
        final TooltipDisplay.Builder builder = TooltipDisplay.tooltipDisplay();
        if (config.hideTooltip()) builder.hideTooltip(true);
        for (String componentId : config.hiddenComponents()) {
            final DataComponentType type = resolveType(componentId);
            if (type != null) builder.addHiddenComponents(type);
        }
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, builder.build());
    }

    public static void clear(ItemStack item) {
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().build());
    }

    public static boolean knowsComponent(String componentId) {
        return resolveType(componentId) != null;
    }

    private static DataComponentType resolveType(String componentId) {
        return TYPE_REGISTRY.get(componentId.replace("minecraft:", "").toLowerCase());
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
                final DataComponentType type = (DataComponentType) field.get(null);
                map.put(type.getKey().value(), type);
            } catch (IllegalAccessException ignored) {
                // Skip fields we cannot read reflectively; the id simply stays unknown.
            }
        }
        return map;
    }
}
