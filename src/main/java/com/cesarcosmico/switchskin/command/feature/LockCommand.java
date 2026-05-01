package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.service.PlayerLockService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public final class LockCommand {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<PlayerLockService> lockSupplier;

    public LockCommand(Supplier<LangConfig> langSupplier,
                       Supplier<PlayerLockService> lockSupplier) {
        this.langSupplier = langSupplier;
        this.lockSupplier = lockSupplier;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("lock")
                .requires(source -> source.getSender().hasPermission("switchskin.use"))
                .executes(this::execute)
                .build();
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getSender() instanceof Player player)) {
            langSupplier.get().send(ctx.getSource().getSender(), "command.only-players");
            return Command.SINGLE_SUCCESS;
        }
        final boolean nowLocked = lockSupplier.get().toggle(player);
        langSupplier.get().send(player, nowLocked ? "command.lock-enabled" : "command.lock-disabled");
        return Command.SINGLE_SUCCESS;
    }
}
