package com.cesarcosmico.skinswitch.item;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
 * Bracket color comes from the skin's `color` when its tooltip is
 * applied, otherwise falls back to skins.yml `default-bracket-color`.
 *
 * Layout knobs (lang):
 *   lore.position      'above' or 'below' the original lore
 *   lore.lines-before  list of lines inserted before the slot row
 *   lore.lines-after   list of lines inserted after the slot row
 *   lore.prefix        text before the first slot in the row
 *   lore.separator     text between slots
 *   lore.suffix        text after the last slot in the row
 *
 * Every user-supplied string is run through the {@link PlaceholderResolver}
 * before MiniMessage parsing so PlaceholderAPI tokens are resolved when
 * that plugin is installed.
 */
public final class LoreRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<PlaceholderResolver> placeholderSupplier;

    public LoreRenderer(Supplier<LangConfig> langSupplier,
                        Supplier<SkinConfig> skinSupplier,
                        Supplier<PlaceholderResolver> placeholderSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.placeholderSupplier = placeholderSupplier;
    }

    public List<Component> render(List<Component> originalLore,
                                  List<String> skinIds,
                                  int currentIndex,
                                  Collection<String> tooltipSkinIds,
                                  @Nullable Player player) {
        if (skinIds.isEmpty()) {
            return originalLore == null ? List.of() : new ArrayList<>(originalLore);
        }

        LangConfig lang = langSupplier.get();
        PlaceholderResolver resolver = placeholderSupplier.get();
        Component slotRow = buildSlotRow(lang, resolver, player, skinIds, currentIndex, tooltipSkinIds);
        boolean above = "above".equalsIgnoreCase(lang.getRaw("lore.position"));

        List<Component> block = new ArrayList<>();
        for (String line : lang.getRawList("lore.lines-before")) {
            block.add(asLoreLine(resolver.resolve(player, line)));
        }
        block.add(slotRow);
        for (String line : lang.getRawList("lore.lines-after")) {
            block.add(asLoreLine(resolver.resolve(player, line)));
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
                                   PlaceholderResolver resolver,
                                   @Nullable Player player,
                                   List<String> skinIds,
                                   int currentIndex,
                                   Collection<String> tooltipSkinIds) {
        SkinConfig skinConfig = skinSupplier.get();
        String defaultColor = skinConfig.getDefaultBracketColor();
        String prefix = resolver.resolve(player, lang.getRaw("lore.prefix"));
        String separator = resolver.resolve(player, lang.getRaw("lore.separator"));
        String suffix = resolver.resolve(player, lang.getRaw("lore.suffix"));
        Set<String> tooltipSet = new HashSet<>(tooltipSkinIds);

        StringBuilder middle = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) middle.append(separator);

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
                color = skinColor != null ? skinColor : defaultColor;
            } else {
                color = defaultColor;
            }

            String key = active ? "lore.slot-active" : "lore.slot-inactive";
            middle.append(lang.getRaw(key)
                    .replace("{color}", color)
                    .replace("{icon}", icon)
                    .replace("{skin}", icon));
        }

        // Build prefix/suffix as explicit text Components instead of baking
        // them into the MiniMessage string — leading/trailing whitespace on
        // the root of a MINI.deserialize result is silently dropped by the
        // Adventure -> NBT pipeline, so we keep them as their own nodes.
        return Component.text("")
                .decoration(TextDecoration.ITALIC, false)
                .append(literal(prefix))
                .append(deserialize(middle.toString()))
                .append(literal(suffix));
    }

    private static Component literal(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return Component.text(raw).decoration(TextDecoration.ITALIC, false);
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }

    // Wraps a deserialized line in an empty parent so that any leading
    // whitespace (which Adventure's NBT serializer drops from the root
    // of a top-level TextComponent) is preserved as a child node.
    private static Component asLoreLine(String raw) {
        return Component.text("")
                .decoration(TextDecoration.ITALIC, false)
                .append(deserialize(raw));
    }
}
