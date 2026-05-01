package com.cesarcosmico.skinswitch.item;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Renders the skin-slot section appended to an item's lore.
 *
 * Output is a single inline row, e.g. "[🌷] [🎃]", separated from the
 * original lore by an empty line. Bracket colors are configurable via
 * skins.yml -> slot-colors (active/inactive); each skin can override the
 * active color via its own 'color' field.
 */
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
        if (skinIds.isEmpty()) {
            return out;
        }
        if (!out.isEmpty()) {
            out.add(Component.empty());
        }

        LangConfig lang = langSupplier.get();
        SkinConfig skinConfig = skinSupplier.get();
        SkinConfig.SlotColors colors = skinConfig.getSlotColors();
        String separator = lang.getRaw("lore.separator");

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) line.append(separator);

            String id = skinIds.get(i);
            Optional<SkinDefinition> skin = skinConfig.get(id);
            String icon = skin.map(SkinDefinition::icon).orElse(id);
            boolean active = i == currentIndex;

            String color;
            if (active) {
                String skinColor = skin.filter(SkinDefinition::hasColor)
                        .map(SkinDefinition::color)
                        .orElse(null);
                color = skinColor != null ? skinColor : colors.active();
            } else {
                color = colors.inactive();
            }

            String key = active ? "lore.slot-active" : "lore.slot-inactive";
            line.append(lang.getRaw(key)
                    .replace("{color}", color)
                    .replace("{icon}", icon)
                    .replace("{skin}", icon));
        }
        out.add(deserialize(line.toString()));
        return out;
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
