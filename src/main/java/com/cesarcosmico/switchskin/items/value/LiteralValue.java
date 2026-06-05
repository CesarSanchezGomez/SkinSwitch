package com.cesarcosmico.switchskin.items.value;

import com.cesarcosmico.switchskin.items.ItemContext;

public record LiteralValue(Object value) implements ComponentValue {

    @Override
    public Object resolve(ItemContext context) {
        return value;
    }
}
