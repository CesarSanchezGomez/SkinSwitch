package com.cesarcosmico.skinswitch;

import com.cesarcosmico.skinswitch.command.CommandManager;
import com.cesarcosmico.skinswitch.config.ConfigVersionChecker;
import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.item.LoreRenderer;
import com.cesarcosmico.skinswitch.item.SkinSlotKeys;
import com.cesarcosmico.skinswitch.item.SkinTokenFactory;
import com.cesarcosmico.skinswitch.listener.SkinSlotListener;
import com.cesarcosmico.skinswitch.service.SkinSlotService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SkinSwitchPlugin extends JavaPlugin {

    private LangConfig langConfig;
    private PluginConfig pluginConfig;
    private SkinConfig skinConfig;
    private SkinSlotService skinSlotService;
    private SkinTokenFactory skinTokenFactory;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigVersionChecker.check(getConfig(), "config.yml",
                PluginConfig.CURRENT_VERSION, this, getLogger());

        saveDefaultIfMissing("skins.yml");
        FileConfiguration skinsYml = YamlConfiguration.loadConfiguration(
                new File(getDataFolder(), "skins.yml"));
        ConfigVersionChecker.check(skinsYml, "skins.yml",
                SkinConfig.CURRENT_VERSION, this, getLogger());

        this.langConfig = new LangConfig(this);
        this.pluginConfig = new PluginConfig(getConfig(), getLogger());
        this.skinConfig = new SkinConfig(skinsYml, getLogger());

        SkinSlotKeys keys = new SkinSlotKeys(this);
        LoreRenderer loreRenderer = new LoreRenderer(this::getLangConfig, this::getSkinConfig);
        this.skinSlotService = new SkinSlotService(keys, loreRenderer,
                this::getSkinConfig, this::getPluginConfig);
        this.skinTokenFactory = new SkinTokenFactory(keys, this::getPluginConfig, this::getSkinConfig);

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new SkinSlotListener(skinSlotService, skinTokenFactory,
                        this::getLangConfig, this::getSkinConfig), this);

        getLogger().info("SkinSwitch enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkinSwitch disabled.");
    }

    public void reload() {
        reloadConfig();
        ConfigVersionChecker.check(getConfig(), "config.yml",
                PluginConfig.CURRENT_VERSION, this, getLogger());

        FileConfiguration skinsYml = YamlConfiguration.loadConfiguration(
                new File(getDataFolder(), "skins.yml"));
        ConfigVersionChecker.check(skinsYml, "skins.yml",
                SkinConfig.CURRENT_VERSION, this, getLogger());

        this.langConfig.load();
        this.pluginConfig = new PluginConfig(getConfig(), getLogger());
        this.skinConfig = new SkinConfig(skinsYml, getLogger());
    }

    private void registerCommands() {
        final CommandManager commandManager = new CommandManager(
                this::getLangConfig,
                this::getSkinConfig,
                this::getPluginConfig,
                this::getSkinSlotService,
                this::getSkinTokenFactory,
                this::reload
        );

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(commandManager.createCommand());
            event.registrar().register(commandManager.createAliasCommand());
        });
    }

    private void saveDefaultIfMissing(String name) {
        File f = new File(getDataFolder(), name);
        if (!f.exists()) {
            saveResource(name, false);
        }
    }

    public LangConfig getLangConfig() { return langConfig; }
    public PluginConfig getPluginConfig() { return pluginConfig; }
    public SkinConfig getSkinConfig() { return skinConfig; }
    public SkinSlotService getSkinSlotService() { return skinSlotService; }
    public SkinTokenFactory getSkinTokenFactory() { return skinTokenFactory; }
}
