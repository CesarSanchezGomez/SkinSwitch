package com.cesarcosmico.switchskin.items;

import com.cesarcosmico.switchskin.adapter.placeholderapi.PlaceholderResolver;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.text.MessageManager;
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

public final class LoreRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String SLOT_SEPARATOR = " ";

    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final PlaceholderResolver placeholderResolver;

    public LoreRenderer(MessageManager messages, SkinConfig skinConfig,
                        PlaceholderResolver placeholderResolver) {
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.placeholderResolver = placeholderResolver;
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
        final Set<String> tooltipSet = new HashSet<>(tooltipSkinIds);

        final StringBuilder slots = new StringBuilder();
        for (int i = 0; i < skinIds.size(); i++) {
            if (i > 0) slots.append(SLOT_SEPARATOR);
            final String id = skinIds.get(i);
            slots.append(renderSlot(id, i == currentIndex, tooltipSet.contains(id)));
        }

        final String row = placeholderResolver.resolve(owner, messages.getRaw("lore.row"))
                .replace("{slots}", slots.toString());
        return MINI.deserialize(row).decoration(TextDecoration.ITALIC, false);
    }

    private String renderSlot(String skinId, boolean active, boolean hasTooltip) {
        final SkinDefinition skin = skinConfig.get(skinId).orElse(null);
        final String icon = resolveIcon(skin, skinId, active);
        final String color = resolveBracketColor(skin, hasTooltip);
        final String template = messages.getRaw(active ? "lore.slot-active" : "lore.slot-inactive");
        return template
                .replace("{color}", color)
                .replace("{icon}", icon)
                .replace("{skin}", icon);
    }

    private String resolveIcon(@Nullable SkinDefinition skin, String skinId, boolean active) {
        if (active) {
            if (skin != null && skin.hasIconActive()) return skin.iconActive();
            final String def = skinConfig.getDefaultIconActive();
            return def != null && !def.isEmpty() ? def : skinId;
        }
        if (skin != null && skin.hasIconInactive()) return skin.iconInactive();
        if (skin != null && skin.hasIconActive()) return skin.iconActive();
        final String def = skinConfig.getDefaultIconInactive();
        if (def != null && !def.isEmpty()) return def;
        final String activeDef = skinConfig.getDefaultIconActive();
        return activeDef != null && !activeDef.isEmpty() ? activeDef : skinId;
    }

    private String resolveBracketColor(@Nullable SkinDefinition skin, boolean hasTooltip) {
        if (hasTooltip) {
            if (skin != null && skin.hasBracketColorActive()) return skin.bracketColorActive();
            return skinConfig.getDefaultBracketColorActive();
        }
        if (skin != null && skin.hasBracketColorInactive()) return skin.bracketColorInactive();
        return skinConfig.getDefaultBracketColorInactive();
    }
}
