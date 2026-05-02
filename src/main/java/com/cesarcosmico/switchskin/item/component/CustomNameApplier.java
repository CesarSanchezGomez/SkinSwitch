package com.cesarcosmico.switchskin.item.component;

import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class CustomNameApplier extends BaseComponentApplier {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    @Override
    public String key() {
        return "custom_name";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final String raw = section.getString(key());
        if (raw == null) return;
        editMeta(item, meta ->
                meta.displayName(MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false)));
    }
}
