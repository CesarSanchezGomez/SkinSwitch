package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class RemoveSlotCommand {

    private final MessageManager messages;
    private final SkinSlotService service;

    public RemoveSlotCommand(MessageManager messages, SkinSlotService service) {
        this.messages = messages;
        this.service = service;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("removeslot")
                .requires(CommandSupport.permission("switchskin.command.removeslot"))
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(this::execute))
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

        final int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        final List<String> slots = service.getSlots(item);
        final String removedDisplay = (index >= 0 && index < slots.size()) ? slots.get(index) : "?";

        final SkinSlotService.RemoveResult result = service.removeSlot(item, index);
        switch (result) {
            case REMOVED -> CommandSupport.send(player, messages, "command.slot-removed",
                    Placeholder.parsed("skin", removedDisplay));
            case INVALID_INDEX -> CommandSupport.send(player, messages, "command.invalid-slot");
            case NO_SLOTS -> CommandSupport.send(player, messages, "command.no-slots");
            case NO_META -> CommandSupport.send(player, messages, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }
}
