package com.cesarcosmico.skinswitch.command.feature;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.service.SkinSlotService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public final class RemoveSlotCommand {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;

    public RemoveSlotCommand(Supplier<LangConfig> langSupplier,
                             Supplier<SkinSlotService> serviceSupplier) {
        this.langSupplier = langSupplier;
        this.serviceSupplier = serviceSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("removeslot")
                .requires(source -> source.getSender().hasPermission("skinswitch.admin"))
                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                        .executes(this::execute))
                .build();
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.only-players");
            return Command.SINGLE_SUCCESS;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            langSupplier.get().send(player, "command.no-item-in-hand");
            return Command.SINGLE_SUCCESS;
        }

        int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        SkinSlotService service = serviceSupplier.get();
        List<String> slots = service.getSlots(item);
        String removedDisplay = (index >= 0 && index < slots.size()) ? slots.get(index) : "?";

        SkinSlotService.RemoveResult result = service.removeSlot(item, index, player);
        switch (result) {
            case REMOVED -> langSupplier.get().send(player, "command.slot-removed",
                    "{skin}", removedDisplay);
            case INVALID_INDEX -> langSupplier.get().send(player, "command.invalid-slot");
            case NO_SLOTS -> langSupplier.get().send(player, "command.no-slots");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
        return Command.SINGLE_SUCCESS;
    }
}
