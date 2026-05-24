package com.cesarcosmico.switchskin.items;

import com.cesarcosmico.switchskin.items.value.ComponentValue;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** A config item: its base material plus the compiled vanilla components to apply. */
public record CompiledItem(@Nullable Material material, Map<String, ComponentValue> components) {

    public static final CompiledItem EMPTY = new CompiledItem(null, Map.of());

    public CompiledItem {
        components = Map.copyOf(components);
    }
}
