package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public final class LoreRenderer {

    public static final String ICONS_PLACEHOLDER = "{icons}";

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

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
                                  @Nullable OfflinePlayer owner) {
        if (skinIds.isEmpty()) {
            return originalLore == null ? List.of() : new ArrayList<>(originalLore);
        }

        final LangConfig lang = langSupplier.get();
        final PlaceholderResolver resolver = placeholderSupplier.get();
        final Component slotRow = buildSlotRow(lang, resolver, owner, skinIds, currentIndex, tooltipSkinIds);
        final TextReplacementConfig replacement = TextReplacementConfig.builder()
                .matchLiteral(ICONS_PLACEHOLDER)
                .replacement(slotRow)
                .build();

        final List<Component> base = originalLore == null ? List.of() : originalLore;
        final List<Component> out = new ArrayList<>(base.size() + 1);
        boolean substituted = false;
        for (Component line : base) {
            if (PLAIN.serialize(line).contains(ICONS_PLACEHOLDER)) {
                out.add(line.replaceText(replacement));
                substituted = true;
            } else {
                out.add(line);
            }
        }
        if (!substituted) out.add(slotRow);
        return out;
    }

    private Component buildSlotRow(LangConfig lang,
                                   PlaceholderResolver resolver,
                                   @Nullable OfflinePlayer owner,
                                   List<String> skinIds,
                                   int currentIndex,
                                   Collection<String> tooltipSkinIds) {
        final SkinConfig skinConfig = skinSupplier.get();
        final String globalDefault = skinConfig.getDefaultBracketColor();
        final String prefix = resolver.resolve(owner, lang.getRaw("lore.prefix"));
        final String separator = resolver.resolve(owner, lang.getRaw("lore.separator"));
        final String suffix = resolver.resolve(owner, lang.getRaw("lore.suffix"));
        final Set<String> tooltipSet = new HashSet<>(tooltipSkinIds);

        final StringBuilder middle = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) middle.append(separator);

            final String id = skinIds.get(i);
            final Optional<SkinDefinition> skin = skinConfig.get(id);
            final boolean active = i == currentIndex;
            final boolean hasTooltip = tooltipSet.contains(id);

            final String icon = skin
                    .map(s -> active ? s.activeIcon() : s.inactiveIcon())
                    .orElse(id);
            final String bracketColor = resolveBracketColor(skin.orElse(null), hasTooltip, globalDefault);

            final String key = active ? "lore.slot-active" : "lore.slot-inactive";
            middle.append(lang.getRaw(key)
                    .replace("{color}", bracketColor)
                    .replace("{icon}", icon)
                    .replace("{skin}", icon));
        }

        return Component.text("")
                .decoration(TextDecoration.ITALIC, false)
                .append(literal(prefix))
                .append(deserialize(middle.toString()))
                .append(literal(suffix));
    }

    private static String resolveBracketColor(@Nullable SkinDefinition skin,
                                              boolean hasTooltip, String globalDefault) {
        if (skin == null) return globalDefault;
        if (hasTooltip) {
            return skin.hasBracketColor() ? skin.bracketColor() : globalDefault;
        }
        return skin.hasBracketColorDefault() ? skin.bracketColorDefault() : globalDefault;
    }

    private static Component literal(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return Component.text(raw).decoration(TextDecoration.ITALIC, false);
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
