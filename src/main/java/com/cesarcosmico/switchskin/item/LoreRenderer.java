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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
                                  Collection<String> tooltipSkinIds,
                                  @Nullable OfflinePlayer owner) {
        final List<Component> base = originalLore == null ? List.of() : originalLore;
        if (skinIds.isEmpty()) return new ArrayList<>(base);

        final Component row = buildRow(skinIds, currentIndex, tooltipSkinIds, owner);
        final List<Component> out = new ArrayList<>(base.size() + 1);
        out.add(row);
        out.addAll(base);
        return out;
    }

    private Component buildRow(List<String> skinIds, int currentIndex,
                               Collection<String> tooltipSkinIds, @Nullable OfflinePlayer owner) {
        final LangConfig lang = langSupplier.get();
        final SkinConfig skinConfig = skinSupplier.get();
        final PlaceholderResolver resolver = placeholderSupplier.get();
        final Set<String> tooltipSet = new HashSet<>(tooltipSkinIds);

        final StringBuilder slots = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) slots.append(SLOT_SEPARATOR);
            final String id = skinIds.get(i);
            slots.append(renderSlot(lang, skinConfig, id, i == currentIndex, tooltipSet.contains(id)));
        }

        final String row = resolver.resolve(owner, lang.getRaw("lore.row"))
                .replace("{slots}", slots.toString());
        return MINI.deserialize(row).decoration(TextDecoration.ITALIC, false);
    }

    private static String renderSlot(LangConfig lang, SkinConfig skinConfig, String skinId,
                                     boolean active, boolean hasTooltip) {
        final SkinDefinition skin = skinConfig.get(skinId).orElse(null);
        final String icon = resolveIcon(skin, skinId, skinConfig, active);
        final String color = resolveBracketColor(skin, skinConfig, active, hasTooltip);
        final String template = lang.getRaw(active ? "lore.slot-active" : "lore.slot-inactive");
        return template
                .replace("{color}", color)
                .replace("{icon}", icon)
                .replace("{skin}", icon);
    }

    private static String resolveIcon(@Nullable SkinDefinition skin, String skinId,
                                      SkinConfig config, boolean active) {
        if (active) {
            if (skin != null && skin.hasIconActive()) return skin.iconActive();
            final String def = config.getDefaultIconActive();
            return def != null && !def.isEmpty() ? def : skinId;
        }
        if (skin != null && skin.hasIconInactive()) return skin.iconInactive();
        if (skin != null && skin.hasIconActive()) return skin.iconActive();
        final String def = config.getDefaultIconInactive();
        if (def != null && !def.isEmpty()) return def;
        final String activeDef = config.getDefaultIconActive();
        return activeDef != null && !activeDef.isEmpty() ? activeDef : skinId;
    }

    private static String resolveBracketColor(@Nullable SkinDefinition skin,
                                              SkinConfig config, boolean active, boolean hasTooltip) {
        if (active && hasTooltip) {
            if (skin != null && skin.hasBracketColorActive()) return skin.bracketColorActive();
            return config.getDefaultBracketColorActive();
        }
        if (skin != null && skin.hasBracketColorInactive()) return skin.bracketColorInactive();
        return config.getDefaultBracketColorInactive();
    }
}
