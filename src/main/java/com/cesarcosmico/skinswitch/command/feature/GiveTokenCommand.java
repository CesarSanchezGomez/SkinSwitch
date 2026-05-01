package com.cesarcosmico.skinswitch.command.feature;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.item.TokenFactory;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Generic /givetoken-style command. Reused for both the skin token
 * (literal "givetoken") and the tooltip token (literal "givetooltip");
 * only the literal name, factory and skin filter differ.
 */
public final class GiveTokenCommand {

    private final String literal;
    private final String successKey;
    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<TokenFactory> tokenSupplier;
    private final Predicate<SkinDefinition> skinFilter;

    public GiveTokenCommand(String literal,
                            String successKey,
                            Supplier<LangConfig> langSupplier,
                            Supplier<SkinConfig> skinSupplier,
                            Supplier<TokenFactory> tokenSupplier,
                            Predicate<SkinDefinition> skinFilter) {
        this.literal = literal;
        this.successKey = successKey;
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.tokenSupplier = tokenSupplier;
        this.skinFilter = skinFilter;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal(literal)
                .requires(source -> source.getSender().hasPermission("skinswitch.admin"))
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

    private CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(input)) builder.suggest(p.getName());
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSkins(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        for (SkinDefinition def : skinSupplier.get().all().values()) {
            if (!skinFilter.test(def)) continue;
            if (def.id().toLowerCase().startsWith(input)) builder.suggest(def.id());
        }
        return builder.buildFuture();
    }

    private int execute(CommandContext<CommandSourceStack> ctx, int amount) {
        String targetName = StringArgumentType.getString(ctx, "player");
        String skinId = StringArgumentType.getString(ctx, "skin");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.player-not-found",
                    "{player}", targetName);
            return Command.SINGLE_SUCCESS;
        }

        SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        if (def == null || !skinFilter.test(def)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.unknown-skin",
                    "{skin}", skinId);
            return Command.SINGLE_SUCCESS;
        }

        ItemStack token = tokenSupplier.get().create(skinId, amount);
        if (token == null) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.unknown-skin",
                    "{skin}", skinId);
            return Command.SINGLE_SUCCESS;
        }

        HashMap<Integer, ItemStack> leftovers = new HashMap<>(target.getInventory().addItem(token));
        for (ItemStack overflow : leftovers.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), overflow);
        }

        langSupplier.get().send(ctx.getSource().getSender(), successKey,
                "{count}", String.valueOf(amount),
                "{skin}", def.displayOrId(),
                "{player}", target.getName());

        return Command.SINGLE_SUCCESS;
    }
}
