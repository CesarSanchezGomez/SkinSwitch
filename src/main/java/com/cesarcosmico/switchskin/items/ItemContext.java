package com.cesarcosmico.switchskin.items;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public record ItemContext(@Nullable UUID playerId, @Nullable String skin, @NotNull Map<String, String> extras) {

    private static final ItemContext EMPTY = new ItemContext(null, null, Map.of());

    public ItemContext {
        extras = Map.copyOf(extras);
    }

    public static ItemContext empty() {
        return EMPTY;
    }

    public static ItemContext forPlayer(@Nullable UUID playerId) {
        return new ItemContext(playerId, null, Map.of());
    }

    public ItemContext withSkin(@Nullable String skin) {
        return new ItemContext(playerId, skin, extras);
    }

    public ItemContext withExtras(@NotNull Map<String, String> extras) {
        return new ItemContext(playerId, skin, extras);
    }

    public TagResolver tagResolver() {
        TagResolver.Builder builder = TagResolver.builder();
        builder.resolver(Placeholder.unparsed("player", playerName()));
        builder.resolver(Placeholder.parsed("skin", skin == null ? "" : skin));
        for (Map.Entry<String, String> entry : extras.entrySet()) {
            builder.resolver(Placeholder.unparsed(entry.getKey(), entry.getValue()));
        }
        return builder.build();
    }

    private String playerName() {
        if (playerId == null) return "";
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) return online.getName();
        String offline = Bukkit.getOfflinePlayer(playerId).getName();
        return offline == null ? "" : offline;
    }
}
