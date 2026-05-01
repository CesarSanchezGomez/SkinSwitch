package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.service.CooldownService;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class SwitchCommand {

    private static final String VANILLA_LITERAL = "vanilla";

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;
    private final Supplier<CooldownService> cooldownSupplier;
    private final Supplier<SwitchAnnouncer> announcerSupplier;

    public SwitchCommand(Supplier<LangConfig> langSupplier,
                         Supplier<SkinConfig> skinSupplier,
                         Supplier<SkinSlotService> serviceSupplier,
                         Supplier<CooldownService> cooldownSupplier,
                         Supplier<SwitchAnnouncer> announcerSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.serviceSupplier = serviceSupplier;
        this.cooldownSupplier = cooldownSupplier;
        this.announcerSupplier = announcerSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("switch")
                .requires(source -> source.getSender().hasPermission("switchskin.use"))
                .executes(this::cycle)
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests(this::suggestTargets)
                        .executes(this::selectByName))
                .build();
    }

    private int cycle(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.only-players");
            return Command.SINGLE_SUCCESS;
        }
        if (!checkCommonPreconditions(player)) return Command.SINGLE_SUCCESS;

        final ItemStack item = player.getInventory().getItemInMainHand();
        final SkinSlotService service = serviceSupplier.get();
        if (!service.hasSlots(item)) {
            langSupplier.get().send(player, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        switch (service.cycleNext(item)) {
            case CYCLED -> {
                player.getInventory().setItemInMainHand(item);
                cooldownSupplier.get().mark(player);
                service.getActiveSkin(item).ifPresent(s -> announcerSupplier.get().announceSwitch(player, s));
            }
            case SINGLE_SLOT -> langSupplier.get().send(player, "command.single-slot");
            case NO_SLOTS, NO_META -> langSupplier.get().send(player, "command.no-slots");
        }
        return Command.SINGLE_SUCCESS;
    }

    private int selectByName(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.only-players");
            return Command.SINGLE_SUCCESS;
        }
        if (!checkCommonPreconditions(player)) return Command.SINGLE_SUCCESS;

        final String requested = StringArgumentType.getString(ctx, "skin");
        if (VANILLA_LITERAL.equalsIgnoreCase(requested)) {
            return selectVanilla(player);
        }

        final ItemStack item = player.getInventory().getItemInMainHand();
        final SkinSlotService service = serviceSupplier.get();
        final List<String> slots = service.getSlots(item);
        if (slots.isEmpty()) {
            langSupplier.get().send(player, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final int index = slots.indexOf(requested);
        if (index < 0) {
            langSupplier.get().send(player, "command.skin-not-on-item", "{skin}", requested);
            return Command.SINGLE_SUCCESS;
        }

        final SkinSlotService.SelectResult result = service.selectIndex(item, index);
        final SkinDefinition def = skinSupplier.get().get(requested).orElse(null);
        final String display = def == null ? requested : def.nameOrId();
        switch (result) {
            case SELECTED -> {
                player.getInventory().setItemInMainHand(item);
                cooldownSupplier.get().mark(player);
                service.getActiveSkin(item).ifPresent(s -> announcerSupplier.get().announceSwitch(player, s));
            }
            case ALREADY_ACTIVE -> langSupplier.get().send(player, "command.already-active",
                    "{skin}", display);
            case INVALID_INDEX, NO_SLOTS, NO_META -> langSupplier.get().send(player, "command.no-slots");
        }
        return Command.SINGLE_SUCCESS;
    }

    private int selectVanilla(Player player) {
        final ItemStack item = player.getInventory().getItemInMainHand();
        switch (serviceSupplier.get().selectVanilla(item)) {
            case APPLIED -> {
                player.getInventory().setItemInMainHand(item);
                cooldownSupplier.get().mark(player);
                announcerSupplier.get().announceVanilla(player);
            }
            case ALREADY_VANILLA -> langSupplier.get().send(player, "command.already-vanilla");
            case NO_SLOTS -> langSupplier.get().send(player, "command.no-slots");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }

    private boolean checkCommonPreconditions(Player player) {
        final CooldownService cooldown = cooldownSupplier.get();
        if (!cooldown.isReady(player)) {
            langSupplier.get().send(player, "command.cooldown",
                    "{seconds}", String.format("%.1f", cooldown.remainingMillis(player) / 1000.0));
            return false;
        }
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            langSupplier.get().send(player, "command.no-item-in-hand");
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
        for (String id : serviceSupplier.get().getSlots(item)) {
            if (id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }
}
