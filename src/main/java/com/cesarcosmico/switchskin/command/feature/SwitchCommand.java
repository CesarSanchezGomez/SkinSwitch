package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.service.CooldownService;
import com.cesarcosmico.switchskin.service.PlayerLockService;
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

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;
    private final Supplier<CooldownService> cooldownSupplier;
    private final Supplier<PlayerLockService> lockSupplier;
    private final Supplier<SwitchAnnouncer> announcerSupplier;

    public SwitchCommand(Supplier<LangConfig> langSupplier,
                         Supplier<SkinConfig> skinSupplier,
                         Supplier<SkinSlotService> serviceSupplier,
                         Supplier<CooldownService> cooldownSupplier,
                         Supplier<PlayerLockService> lockSupplier,
                         Supplier<SwitchAnnouncer> announcerSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.serviceSupplier = serviceSupplier;
        this.cooldownSupplier = cooldownSupplier;
        this.lockSupplier = lockSupplier;
        this.announcerSupplier = announcerSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("switch")
                .requires(source -> source.getSender().hasPermission("switchskin.use"))
                .executes(this::cycle)
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests(this::suggestHeldItemSlots)
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

        final SkinSlotService.CycleResult result = service.cycleNext(item);
        switch (result) {
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

        final ItemStack item = player.getInventory().getItemInMainHand();
        final SkinSlotService service = serviceSupplier.get();
        final List<String> slots = service.getSlots(item);
        if (slots.isEmpty()) {
            langSupplier.get().send(player, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final String requested = StringArgumentType.getString(ctx, "skin");
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

    private boolean checkCommonPreconditions(Player player) {
        if (lockSupplier.get().isLocked(player)) {
            langSupplier.get().send(player, "command.switch-locked");
            return false;
        }
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

    private CompletableFuture<Suggestions> suggestHeldItemSlots(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            return builder.buildFuture();
        }
        final ItemStack item = player.getInventory().getItemInMainHand();
        final List<String> slots = serviceSupplier.get().getSlots(item);
        final String input = builder.getRemaining().toLowerCase();
        for (String id : slots) {
            if (id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }
}
