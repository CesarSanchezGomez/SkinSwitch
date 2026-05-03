package com.cesarcosmico.switchskin.placeholder;

import org.bukkit.OfflinePlayer;

public final class NoopPlaceholderResolver implements PlaceholderResolver {

    @Override
    public String resolve(OfflinePlayer owner, String input) {
        return input;
    }
}
