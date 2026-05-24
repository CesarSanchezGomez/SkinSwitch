package com.cesarcosmico.switchskin.items.value;

import com.cesarcosmico.switchskin.items.ItemContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public record ListValue(List<ComponentValue> elements) implements ComponentValue {

    @Override
    public Object resolve(ItemContext context) {
        List<Object> resolved = new ArrayList<>(elements.size());
        boolean allJson = !elements.isEmpty();
        for (ComponentValue value : elements) {
            Object r = value.resolve(context);
            resolved.add(r);
            if (allJson && !(r instanceof JsonElement)) {
                allJson = false;
            }
        }
        if (allJson) {
            JsonArray array = new JsonArray(resolved.size());
            for (Object element : resolved) {
                array.add((JsonElement) element);
            }
            return array;
        }
        return resolved;
    }
}
