package com.cesarcosmico.skinswitch.placeholder;

import org.bukkit.entity.Player;

public final class NoopPlaceholderResolver implements PlaceholderResolver {

    @Override
    public String resolve(Player player, String input) {
        return input;
    }
}
