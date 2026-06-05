package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SwitchCommand {

    private static final String VANILLA_LITERAL = "vanilla";

    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final SkinSlotService service;
    private final SwitchAnnouncer announcer;

    public SwitchCommand(MessageManager messages, SkinConfig skinConfig,
                         SkinSlotService service, SwitchAnnouncer announcer) {
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.service = service;
        this.announcer = announcer;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("switch")
                .requires(CommandSupport.permission("switchskin.command.switch"))
                .executes(this::cycle)
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests(this::suggestTargets)
                        .executes(this::selectByName))
                .build();
    }

    private int cycle(CommandContext<CommandSourceStack> ctx) {
        final Player player = CommandSupport.requirePlayer(ctx.getSource(), messages);
        if (player == null) return Command.SINGLE_SUCCESS;
        if (!hasItemInHand(player)) return Command.SINGLE_SUCCESS;

        final ItemStack item = player.getInventory().getItemInMainHand();
        if (!service.hasSlots(item)) {
            CommandSupport.send(player, messages, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        switch (service.cycleNext(item)) {
            case CYCLED -> {
                player.getInventory().setItemInMainHand(item);
                service.getActiveSkin(item).ifPresent(s -> announcer.announceSwitch(player, s));
            }
            case SINGLE_SLOT -> CommandSupport.send(player, messages, "command.single-slot");
            case NO_SLOTS, NO_META -> CommandSupport.send(player, messages, "command.no-slots");
        }
        return Command.SINGLE_SUCCESS;
    }

    private int selectByName(CommandContext<CommandSourceStack> ctx) {
        final Player player = CommandSupport.requirePlayer(ctx.getSource(), messages);
        if (player == null) return Command.SINGLE_SUCCESS;
        if (!hasItemInHand(player)) return Command.SINGLE_SUCCESS;

        final String requested = StringArgumentType.getString(ctx, "skin");
        if (VANILLA_LITERAL.equalsIgnoreCase(requested)) {
            return selectVanilla(player);
        }

        final ItemStack item = player.getInventory().getItemInMainHand();
        final List<String> slots = service.getSlots(item);
        if (slots.isEmpty()) {
            CommandSupport.send(player, messages, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final int index = slots.indexOf(requested);
        if (index < 0) {
            CommandSupport.send(player, messages, "command.skin-not-on-item",
                    Placeholder.parsed("skin", requested));
            return Command.SINGLE_SUCCESS;
        }

        final SkinSlotService.SelectResult result = service.selectIndex(item, index);
        final SkinDefinition def = skinConfig.get(requested).orElse(null);
        final String display = def == null ? requested : def.nameOrId();
        switch (result) {
            case SELECTED -> {
                player.getInventory().setItemInMainHand(item);
                service.getActiveSkin(item).ifPresent(s -> announcer.announceSwitch(player, s));
            }
            case ALREADY_ACTIVE -> CommandSupport.send(player, messages, "command.already-active",
                    Placeholder.parsed("skin", display));
            case INVALID_INDEX, NO_SLOTS, NO_META -> CommandSupport.send(player, messages, "command.no-slots");
        }
        return Command.SINGLE_SUCCESS;
    }

    private int selectVanilla(Player player) {
        final ItemStack item = player.getInventory().getItemInMainHand();
        switch (service.selectVanilla(item)) {
            case APPLIED -> {
                player.getInventory().setItemInMainHand(item);
                announcer.announceVanilla(player);
            }
            case ALREADY_VANILLA -> CommandSupport.send(player, messages, "command.already-vanilla");
            case NO_SLOTS -> CommandSupport.send(player, messages, "command.no-slots");
            case NO_META -> CommandSupport.send(player, messages, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }

    private boolean hasItemInHand(Player player) {
        if (player.getInventory().getItemInMainHand().getType().isAir()) {
            CommandSupport.send(player, messages, "command.no-item-in-hand");
            return false;
        }
        return true;
    }

    private CompletableFuture<Suggestions> suggestTargets(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            return builder.buildFuture();
        }
        final String input = builder.getRemaining().toLowerCase();
        if (VANILLA_LITERAL.startsWith(input)) builder.suggest(VANILLA_LITERAL);

        final ItemStack item = player.getInventory().getItemInMainHand();
        for (String id : service.getSlots(item)) {
            if (id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }
}
