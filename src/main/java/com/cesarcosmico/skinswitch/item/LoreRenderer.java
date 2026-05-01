package com.cesarcosmico.skinswitch.item;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Renders the skin-slot row appended to the item lore.
 *
 * Bracket color follows the per-slot tooltip token: a slot with its
 * tooltip applied uses the skin's color (or slot-colors.active), one
 * without uses slot-colors.inactive. Active vs inactive is conveyed via
 * the lang templates (default: bold icon for active).
 *
 * Placement of the slot row is fully driven by lang config:
 *   lore.position      — 'above' or 'below' the original lore
 *   lore.lines-before  — list of MiniMessage lines inserted before the row
 *   lore.lines-after   — list of MiniMessage lines inserted after the row
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
                                  int currentIndex,
                                  Collection<String> tooltipSkinIds) {
        if (skinIds.isEmpty()) {
            return originalLore == null ? List.of() : new ArrayList<>(originalLore);
        }

        LangConfig lang = langSupplier.get();
        Component slotRow = buildSlotRow(lang, skinIds, currentIndex, tooltipSkinIds);
        boolean above = "above".equalsIgnoreCase(lang.getRaw("lore.position"));

        List<Component> block = new ArrayList<>();
        for (String line : lang.getRawList("lore.lines-before")) {
            block.add(deserialize(line));
        }
        block.add(slotRow);
        for (String line : lang.getRawList("lore.lines-after")) {
            block.add(deserialize(line));
        }

        List<Component> out = new ArrayList<>();
        if (above) {
            out.addAll(block);
            if (originalLore != null) out.addAll(originalLore);
        } else {
            if (originalLore != null) out.addAll(originalLore);
            out.addAll(block);
        }
        return out;
    }

    private Component buildSlotRow(LangConfig lang,
                                   List<String> skinIds,
                                   int currentIndex,
                                   Collection<String> tooltipSkinIds) {
        SkinConfig skinConfig = skinSupplier.get();
        SkinConfig.SlotColors colors = skinConfig.getSlotColors();
        String separator = lang.getRaw("lore.separator");
        Set<String> tooltipSet = new HashSet<>(tooltipSkinIds);

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) line.append(separator);

            String id = skinIds.get(i);
            Optional<SkinDefinition> skin = skinConfig.get(id);
            String icon = skin.map(SkinDefinition::icon).orElse(id);
            boolean active = i == currentIndex;
            boolean hasTooltip = tooltipSet.contains(id);

            String color;
            if (hasTooltip) {
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
        return deserialize(line.toString());
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
