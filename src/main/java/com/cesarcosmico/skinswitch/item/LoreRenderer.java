package com.cesarcosmico.skinswitch.item;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class LoreRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;

    public LoreRenderer(Supplier<LangConfig> langSupplier,
                        Supplier<SkinConfig> skinSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
    }

    public List<Component> render(List<Component> originalLore,
                                  List<String> skinIds,
                                  int currentIndex) {
        List<Component> out = new ArrayList<>();
        if (originalLore != null) {
            out.addAll(originalLore);
        }
        if (!out.isEmpty()) {
            out.add(Component.empty());
        }

        LangConfig lang = langSupplier.get();
        SkinConfig skinConfig = skinSupplier.get();

        out.add(deserialize(lang.getRaw("lore.header")));

        for (int i = 0; i < skinIds.size(); i++) {
            String id = skinIds.get(i);
            String displayName = skinConfig.get(id)
                    .map(SkinDefinition::display)
                    .orElse(id);
            String key = i == currentIndex ? "lore.slot-active" : "lore.slot-inactive";
            String raw = lang.getRaw(key).replace("{skin}", displayName);
            out.add(deserialize(raw));
        }
        return out;
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
