package com.cesarcosmico.switchskin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class MessageUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtils() {}

    public static Component parse(String text) {
        return MINI_MESSAGE.deserialize(text);
    }

    public static Component parse(String text, TagResolver resolver) {
        return MINI_MESSAGE.deserialize(text, resolver);
    }
}
