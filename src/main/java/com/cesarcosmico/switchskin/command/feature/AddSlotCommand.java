package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

public final class AddSlotCommand {

    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final SkinSlotService service;

    public AddSlotCommand(MessageManager messages, SkinConfig skinConfig, SkinSlotService service) {
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.service = service;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("addslot")
                .requires(CommandSupport.permission("switchskin.command.addslot"))
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests(this::suggestSkins)
                        .executes(this::execute))
                .build();
    }

    private CompletableFuture<Suggestions> suggestSkins(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        final String input = builder.getRemaining().toLowerCase();
        for (String id : skinConfig.all().keySet()) {
            if (id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        final Player player = CommandSupport.requirePlayer(ctx.getSource(), messages);
        if (player == null) return Command.SINGLE_SUCCESS;

        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            CommandSupport.send(player, messages, "command.no-item-in-hand");
            return Command.SINGLE_SUCCESS;
        }

        final String skinId = StringArgumentType.getString(ctx, "skin");
        final SkinSlotService.AddResult result = service.addSlot(item, skinId, player);
        final SkinDefinition def = skinConfig.get(skinId).orElse(null);
        final String display = def == null ? skinId : def.nameOrId();

        switch (result) {
            case ADDED -> CommandSupport.send(player, messages, "command.slot-added", TagResolver.resolver(
                    Placeholder.parsed("skin", display),
                    Placeholder.unparsed("count", String.valueOf(service.getSlots(item).size()))));
            case UNKNOWN_SKIN -> CommandSupport.send(player, messages, "command.unknown-skin",
                    Placeholder.parsed("skin", skinId));
            case FULL -> CommandSupport.send(player, messages, "command.slots-full",
                    Placeholder.unparsed("max", String.valueOf(service.getSlots(item).size())));
            case DUPLICATE -> CommandSupport.send(player, messages, "command.duplicate-slot");
            case INCOMPATIBLE -> CommandSupport.send(player, messages, "command.skin-incompatible", TagResolver.resolver(
                    Placeholder.parsed("skin", display),
                    Placeholder.unparsed("material", item.getType().getKey().value())));
            case NO_META -> CommandSupport.send(player, messages, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }
}
