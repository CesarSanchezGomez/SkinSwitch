package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class RemoveTooltipCommand {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;

    public RemoveTooltipCommand(Supplier<LangConfig> langSupplier,
                                Supplier<SkinSlotService> serviceSupplier) {
        this.langSupplier = langSupplier;
        this.serviceSupplier = serviceSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("removetooltip")
                .requires(source -> source.getSender().hasPermission("switchskin.admin"))
                .executes(this::execute)
                .build();
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

        final SkinSlotService.TooltipRemoveResult result = serviceSupplier.get().removeTooltip(item);
        switch (result) {
            case REMOVED -> langSupplier.get().send(player, "command.tooltip-removed");
            case NOT_APPLIED -> langSupplier.get().send(player, "command.tooltip-not-applied");
            case NO_SLOTS -> langSupplier.get().send(player, "command.no-slots");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }
}
