package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.config.TokenVisualConfig;
import com.cesarcosmico.switchskin.items.ItemContext;
import com.cesarcosmico.switchskin.items.ItemFactory;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public final class GiveTokenCommand {

    public enum Kind { SKIN, TOOLTIP }

    private final String literal;
    private final String successKey;
    private final Kind kind;
    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final PluginConfig pluginConfig;
    private final ItemFactory itemFactory;

    public GiveTokenCommand(String literal, String successKey, Kind kind,
                            MessageManager messages, SkinConfig skinConfig,
                            PluginConfig pluginConfig, ItemFactory itemFactory) {
        this.literal = literal;
        this.successKey = successKey;
        this.kind = kind;
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.pluginConfig = pluginConfig;
        this.itemFactory = itemFactory;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        final String permission = kind == Kind.SKIN
                ? "switchskin.command.givetokenskin" : "switchskin.command.givetokentooltip";
        return Commands.literal(literal)
                .requires(CommandSupport.permission(permission))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(this::suggestPlayers)
                        .then(Commands.argument("skin", StringArgumentType.word())
                                .suggests(this::suggestSkins)
                                .executes(ctx -> execute(ctx, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "amount")))
                                )
                        )
                )
                .build();
    }

    private boolean accepts(SkinDefinition def) {
        return kind == Kind.SKIN || def.tooltipStyle() != null;
    }

    private CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        final String input = builder.getRemaining().toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(input)) builder.suggest(p.getName());
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSkins(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        final String input = builder.getRemaining().toLowerCase();
        for (SkinDefinition def : skinConfig.all().values()) {
            if (!accepts(def)) continue;
            if (def.id().toLowerCase().startsWith(input)) builder.suggest(def.id());
        }
        return builder.buildFuture();
    }

    private int execute(CommandContext<CommandSourceStack> ctx, int amount) {
        final String targetName = StringArgumentType.getString(ctx, "player");
        final String skinId = StringArgumentType.getString(ctx, "skin");

        final Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            CommandSupport.send(ctx.getSource().getSender(), messages, "command.player-not-found",
                    Placeholder.unparsed("player", targetName));
            return Command.SINGLE_SUCCESS;
        }

        final SkinDefinition def = skinConfig.get(skinId).orElse(null);
        if (def == null || !accepts(def)) {
            CommandSupport.send(ctx.getSource().getSender(), messages, "command.unknown-skin",
                    Placeholder.parsed("skin", skinId));
            return Command.SINGLE_SUCCESS;
        }

        final ItemStack token = buildToken(def, amount, target);

        final HashMap<Integer, ItemStack> leftovers = new HashMap<>(target.getInventory().addItem(token));
        for (ItemStack overflow : leftovers.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), overflow);
        }

        CommandSupport.send(ctx.getSource().getSender(), messages, successKey, TagResolver.resolver(
                Placeholder.unparsed("count", String.valueOf(amount)),
                Placeholder.parsed("skin", def.nameOrId()),
                Placeholder.unparsed("player", target.getName())));
        return Command.SINGLE_SUCCESS;
    }

    private ItemStack buildToken(SkinDefinition def, int amount, Player target) {
        final ItemContext context = ItemContext.forPlayer(target.getUniqueId()).withSkin(def.nameOrId());
        if (kind == Kind.SKIN) {
            final TokenVisualConfig override = def.tokenSkin();
            return itemFactory.buildSkinToken(pluginConfig.getToken(), context, amount, def.id(), override);
        }
        final TokenVisualConfig override = def.tokenTooltip();
        return itemFactory.buildTooltipToken(pluginConfig.getTooltipToken(), context, amount, def.id(), override);
    }
}
