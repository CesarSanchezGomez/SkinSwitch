package com.cesarcosmico.switchskin.command;

import com.cesarcosmico.switchskin.command.feature.AddSlotCommand;
import com.cesarcosmico.switchskin.command.feature.GiveTokenCommand;
import com.cesarcosmico.switchskin.command.feature.LockCommand;
import com.cesarcosmico.switchskin.command.feature.MenuCommand;
import com.cesarcosmico.switchskin.command.feature.ReloadCommand;
import com.cesarcosmico.switchskin.command.feature.RemoveSlotCommand;
import com.cesarcosmico.switchskin.command.feature.RemoveTooltipCommand;
import com.cesarcosmico.switchskin.command.feature.SwitchCommand;
import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.item.TokenFactory;
import com.cesarcosmico.switchskin.service.CooldownService;
import com.cesarcosmico.switchskin.service.PlayerLockService;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.Supplier;

public final class CommandManager {

    private final ReloadCommand reloadCommand;
    private final AddSlotCommand addSlotCommand;
    private final RemoveSlotCommand removeSlotCommand;
    private final GiveTokenCommand giveSkinTokenCommand;
    private final GiveTokenCommand giveTooltipTokenCommand;
    private final RemoveTooltipCommand removeTooltipCommand;
    private final MenuCommand menuCommand;
    private final SwitchCommand switchCommand;
    private final LockCommand lockCommand;

    public CommandManager(Supplier<LangConfig> langSupplier,
                          Supplier<SkinConfig> skinSupplier,
                          Supplier<PluginConfig> pluginSupplier,
                          Supplier<SkinSlotService> serviceSupplier,
                          Supplier<TokenFactory> skinTokenSupplier,
                          Supplier<TokenFactory> tooltipTokenSupplier,
                          Supplier<CooldownService> cooldownSupplier,
                          Supplier<PlayerLockService> lockSupplier,
                          Supplier<SwitchAnnouncer> announcerSupplier,
                          JavaPlugin plugin,
                          Runnable reloadAction) {
        this.reloadCommand = new ReloadCommand(langSupplier, reloadAction);
        this.addSlotCommand = new AddSlotCommand(langSupplier, skinSupplier, serviceSupplier);
        this.removeSlotCommand = new RemoveSlotCommand(langSupplier, serviceSupplier);
        this.giveSkinTokenCommand = new GiveTokenCommand("givetoken", "command.token-given",
                langSupplier, skinSupplier, skinTokenSupplier, def -> true);
        this.giveTooltipTokenCommand = new GiveTokenCommand("givetooltip", "command.tooltip-token-given",
                langSupplier, skinSupplier, tooltipTokenSupplier,
                (SkinDefinition def) -> def.tooltipStyle() != null);
        this.removeTooltipCommand = new RemoveTooltipCommand(langSupplier, serviceSupplier);
        this.menuCommand = new MenuCommand(langSupplier, skinSupplier, pluginSupplier, serviceSupplier);
        this.switchCommand = new SwitchCommand(langSupplier, skinSupplier, serviceSupplier,
                cooldownSupplier, lockSupplier, announcerSupplier);
        this.lockCommand = new LockCommand(langSupplier, lockSupplier);
    }

    public LiteralCommandNode<CommandSourceStack> createCommand() {
        return build("switchskin");
    }

    public LiteralCommandNode<CommandSourceStack> createAliasCommand() {
        return build("ss");
    }

    private LiteralCommandNode<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(source -> source.getSender().hasPermission("switchskin.use"))
                .executes(menuCommand::execute)
                .then(reloadCommand.create())
                .then(addSlotCommand.create())
                .then(removeSlotCommand.create())
                .then(giveSkinTokenCommand.create())
                .then(giveTooltipTokenCommand.create())
                .then(removeTooltipCommand.create())
                .then(menuCommand.create())
                .then(switchCommand.create())
                .then(lockCommand.create())
                .build();
    }
}
