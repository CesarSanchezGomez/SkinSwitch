package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.service.SkinSlotService;
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

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class AddSlotCommand {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;

    public AddSlotCommand(Supplier<LangConfig> langSupplier,
                          Supplier<SkinConfig> skinSupplier,
                          Supplier<SkinSlotService> serviceSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.serviceSupplier = serviceSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("addslot")
                .requires(source -> source.getSender().hasPermission("switchskin.admin"))
                .then(Commands.argument("skin", StringArgumentType.word())
                        .suggests(this::suggestSkins)
                        .executes(this::execute))
                .build();
    }

    private CompletableFuture<Suggestions> suggestSkins(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        final String input = builder.getRemaining().toLowerCase();
        for (String id : skinSupplier.get().all().keySet()) {
            if (id.toLowerCase().startsWith(input)) builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.only-players");
            return Command.SINGLE_SUCCESS;
        }

        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            langSupplier.get().send(player, "command.no-item-in-hand");
            return Command.SINGLE_SUCCESS;
        }

        final String skinId = StringArgumentType.getString(ctx, "skin");
        final SkinSlotService service = serviceSupplier.get();
        final SkinSlotService.AddResult result = service.addSlot(item, skinId, player);
        final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        final String display = def == null ? skinId : def.nameOrId();

        switch (result) {
            case ADDED -> langSupplier.get().send(player, "command.slot-added",
                    "{skin}", display,
                    "{count}", String.valueOf(service.getSlots(item).size()));
            case UNKNOWN_SKIN -> langSupplier.get().send(player, "command.unknown-skin",
                    "{skin}", skinId);
            case FULL -> langSupplier.get().send(player, "command.slots-full",
                    "{max}", String.valueOf(service.getSlots(item).size()));
            case DUPLICATE -> langSupplier.get().send(player, "command.duplicate-slot");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }
}
