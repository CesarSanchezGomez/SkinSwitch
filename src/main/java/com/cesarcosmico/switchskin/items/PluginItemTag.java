package com.cesarcosmico.switchskin.items;

import com.saicone.rtag.RtagItem;
import com.saicone.rtag.data.ComponentType;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PluginItemTag {

    private static final String COMPONENT = "minecraft:custom_data";
    private static final String SKIN_TOKEN_FIELD = "skinswitch_skin_token";
    private static final String TOOLTIP_TOKEN_FIELD = "skinswitch_tooltip_token";

    private PluginItemTag() {}

    public static void writeSkinToken(RtagItem item, String skinId) {
        put(item, SKIN_TOKEN_FIELD, skinId);
    }

    public static void writeTooltipToken(RtagItem item, String skinId) {
        put(item, TOOLTIP_TOKEN_FIELD, skinId);
    }

    public static Optional<String> readSkinToken(ItemStack stack) {
        return read(stack, SKIN_TOKEN_FIELD);
    }

    public static Optional<String> readTooltipToken(ItemStack stack) {
        return read(stack, TOOLTIP_TOKEN_FIELD);
    }

    private static void put(RtagItem item, String field, String value) {
        Map<String, Object> next = new LinkedHashMap<>(readMap(item));
        next.put(field, value);
        item.setComponent(COMPONENT, next);
    }

    private static Optional<String> read(ItemStack stack, String field) {
        if (stack == null || stack.getType().isAir()) return Optional.empty();
        Object value = readMap(new RtagItem(stack)).get(field);
        return value instanceof String s && !s.isEmpty() ? Optional.of(s) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(RtagItem item) {
        Object component = item.getComponent(COMPONENT);
        if (component == null) return Map.of();
        Object encoded = ComponentType.encodeJava(COMPONENT, component).orElse(null);
        if (encoded instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }
}
