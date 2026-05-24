package com.cesarcosmico.switchskin.items;

import com.cesarcosmico.switchskin.items.value.ComponentValue;
import com.saicone.rtag.RtagItem;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class ComponentDispatcher {

    // Keys handled outside the generic dispatch: 'material' defines the stack,
    // 'profile' is applied via the Paper profile API, 'symbol' is a menu layout key.
    private static final Set<String> SKIPPED = Set.of("material", "profile", "symbol");
    private static final String VANILLA_NAMESPACE = "minecraft:";

    private final Logger logger;

    public ComponentDispatcher(Logger logger) {
        this.logger = logger;
    }

    public void apply(RtagItem item, Map<String, ComponentValue> compiled, ItemContext context) {
        for (Map.Entry<String, ComponentValue> entry : compiled.entrySet()) {
            String key = entry.getKey();
            if (SKIPPED.contains(key)) continue;

            String fqn = key.contains(":") ? key : VANILLA_NAMESPACE + key;
            try {
                item.setComponent(fqn, entry.getValue().resolve(context));
            } catch (RuntimeException ex) {
                logger.warning("Failed to apply component " + fqn + ": " + ex.getMessage());
            }
        }
    }
}
