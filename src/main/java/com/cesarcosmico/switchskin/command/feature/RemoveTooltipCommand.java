package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class RemoveTooltipCommand {

    private final MessageManager messages;
    private final SkinSlotService service;

    public RemoveTooltipCommand(MessageManager messages, SkinSlotService service) {
        this.messages = messages;
        this.service = service;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("removetooltip")
                .requires(CommandSupport.permission("switchskin.command.removetooltip"))
                .executes(this::execute)
                .build();
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        final Player player = CommandSupport.requirePlayer(ctx.getSource(), messages);
        if (player == null) return Command.SINGLE_SUCCESS;

        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            CommandSupport.send(player, messages, "command.no-item-in-hand");
            return Command.SINGLE_SUCCESS;
        }

        final SkinSlotService.TooltipRemoveResult result = service.removeTooltip(item);
        switch (result) {
            case REMOVED -> CommandSupport.send(player, messages, "command.tooltip-removed");
            case NOT_APPLIED -> CommandSupport.send(player, messages, "command.tooltip-not-applied");
            case NO_SLOTS -> CommandSupport.send(player, messages, "command.no-slots");
            case NO_META -> CommandSupport.send(player, messages, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }
}
