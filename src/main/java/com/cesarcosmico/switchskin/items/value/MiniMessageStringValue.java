package com.cesarcosmico.switchskin.items.value;

import com.cesarcosmico.switchskin.items.ItemContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public record MiniMessageStringValue(String raw) implements ComponentValue {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    @Override
    public Object resolve(ItemContext context) {
        Component parsed = MINI.deserialize(raw, context.tagResolver())
                .decoration(TextDecoration.ITALIC, false);
        return GSON.serializeToTree(parsed);
    }
}
