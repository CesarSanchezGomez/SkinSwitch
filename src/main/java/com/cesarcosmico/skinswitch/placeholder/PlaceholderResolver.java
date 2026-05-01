package com.cesarcosmico.skinswitch.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves third-party placeholders (PlaceholderAPI) inside skin
 * strings before they are parsed by MiniMessage.
 *
 * Implementations are chosen at plugin enable: if PlaceholderAPI is
 * present {@link PlaceholderApiResolver} is used, otherwise
 * {@link NoopPlaceholderResolver} acts as a transparent pass-through.
 */
public interface PlaceholderResolver {

    String resolve(@Nullable Player player, String input);
}
