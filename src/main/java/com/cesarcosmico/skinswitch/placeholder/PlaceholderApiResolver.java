package com.cesarcosmico.skinswitch.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

public final class PlaceholderApiResolver implements PlaceholderResolver {

    @Override
    public String resolve(OfflinePlayer owner, String input) {
        if (input == null || input.isEmpty()) return input;
        return PlaceholderAPI.setPlaceholders(owner, input);
    }
}
