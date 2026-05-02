package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public final class MenuCommand {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<PluginConfig> pluginSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;

    public MenuCommand(Supplier<LangConfig> langSupplier,
                       Supplier<SkinConfig> skinSupplier,
                       Supplier<PluginConfig> pluginSupplier,
                       Supplier<SkinSlotService> serviceSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.pluginSupplier = pluginSupplier;
        this.serviceSupplier = serviceSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("menu")
                .requires(source -> source.getSender().hasPermission("switchskin.use"))
                .executes(this::execute)
                .build();
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.only-players");
            return Command.SINGLE_SUCCESS;
        }

        final SkinSlotService service = serviceSupplier.get();
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (!service.hasSlots(item)) {
            langSupplier.get().send(player, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final List<String> slots = service.getSlots(item);
        if (slots.isEmpty()) {
            langSupplier.get().send(player, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final int activeIndex = service.getCurrentIndex(item);
        new SkinMenuGUI(pluginSupplier.get().getMenu(), skinSupplier.get(),
                slots, activeIndex, 0, item.getType()).open(player);
        return Command.SINGLE_SUCCESS;
    }
}
