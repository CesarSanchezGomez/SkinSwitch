package com.cesarcosmico.switchskin.placeholder;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public interface PlaceholderResolver {

    String resolve(@Nullable OfflinePlayer owner, String input);
}
