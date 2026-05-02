package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class LoreRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String SLOT_SEPARATOR = " ";

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
                                  @Nullable OfflinePlayer owner) {
        final List<Component> base = originalLore == null ? List.of() : originalLore;
        if (skinIds.isEmpty()) return new ArrayList<>(base);

        final Component row = buildRow(skinIds, currentIndex, owner);
        final List<Component> out = new ArrayList<>(base.size() + 1);
        out.add(row);
        out.addAll(base);
        return out;
    }

    private Component buildRow(List<String> skinIds, int currentIndex, @Nullable OfflinePlayer owner) {
        final LangConfig lang = langSupplier.get();
        final SkinConfig skinConfig = skinSupplier.get();
        final PlaceholderResolver resolver = placeholderSupplier.get();
        final String globalDefaultColor = skinConfig.getDefaultBracketColor();

        final StringBuilder slots = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) slots.append(SLOT_SEPARATOR);
            slots.append(renderSlot(lang, skinConfig, skinIds.get(i),
                    i == currentIndex, globalDefaultColor));
        }

        final String row = resolver.resolve(owner, lang.getRaw("lore.row"))
                .replace("{slots}", slots.toString());
        return MINI.deserialize(row).decoration(TextDecoration.ITALIC, false);
    }

    private static String renderSlot(LangConfig lang, SkinConfig skinConfig, String skinId,
                                     boolean active, String defaultColor) {
        final SkinDefinition skin = skinConfig.get(skinId).orElse(null);
        final String icon = skin != null
                ? (active ? skin.activeIcon() : skin.inactiveIcon())
                : skinId;
        final String color = resolveBracketColor(skin, active, defaultColor);
        final String template = lang.getRaw(active ? "lore.slot-active" : "lore.slot-inactive");
        return template
                .replace("{color}", color)
                .replace("{icon}", icon)
                .replace("{skin}", icon);
    }

    private static String resolveBracketColor(@Nullable SkinDefinition skin,
                                              boolean active, String defaultColor) {
        if (skin == null) return defaultColor;
        if (active) return skin.hasBracketColor() ? skin.bracketColor() : defaultColor;
        return skin.hasBracketColorDefault() ? skin.bracketColorDefault() : defaultColor;
    }
}
