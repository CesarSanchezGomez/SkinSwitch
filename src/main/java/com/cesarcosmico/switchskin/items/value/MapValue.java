package com.cesarcosmico.switchskin.items.value;

import com.cesarcosmico.switchskin.items.ItemContext;

import java.util.LinkedHashMap;
import java.util.Map;

public record MapValue(Map<String, ComponentValue> children) implements ComponentValue {

    @Override
    public Object resolve(ItemContext context) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, ComponentValue> entry : children.entrySet()) {
            out.put(entry.getKey(), entry.getValue().resolve(context));
        }
        return out;
    }
}
