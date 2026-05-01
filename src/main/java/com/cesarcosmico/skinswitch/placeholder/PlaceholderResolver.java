package com.cesarcosmico.skinswitch.placeholder;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves third-party placeholders (PlaceholderAPI) inside skin
 * strings before they are parsed by MiniMessage.
 *
 * Implementations are chosen at plugin enable: if PlaceholderAPI is
 * present {@link PlaceholderApiResolver} is used, otherwise
 * {@link NoopPlaceholderResolver} acts as a transparent pass-through.
 *
 * The resolver is invoked with the item's bound owner (captured the
 * first time a slot is added), not with whoever happens to interact
 * with the item later — that way trades and lookups don't rewrite the
 * lore with another player's data.
 */
public interface PlaceholderResolver {

    String resolve(@Nullable OfflinePlayer owner, String input);
}
