package com.cesarcosmico.skinswitch.placeholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public final class PlaceholderApiResolver implements PlaceholderResolver {

    @Override
    public String resolve(Player player, String input) {
        if (input == null || input.isEmpty()) return input;
        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
