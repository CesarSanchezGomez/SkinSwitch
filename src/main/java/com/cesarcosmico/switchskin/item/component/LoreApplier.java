package com.cesarcosmico.switchskin.item.component;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class LoreApplier extends BaseComponentApplier {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    @Override
    public String key() {
        return "lore";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        final List<String> lines = section.getStringList(key());
        if (lines.isEmpty()) return;

        final List<Component> lore = lines.stream()
                .map(line -> MINI.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .toList();
        editMeta(item, meta -> meta.lore(lore));
    }
}
