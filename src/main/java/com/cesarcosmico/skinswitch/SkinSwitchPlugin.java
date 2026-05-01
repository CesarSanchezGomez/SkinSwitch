package com.cesarcosmico.skinswitch;

import com.cesarcosmico.skinswitch.command.CommandManager;
import com.cesarcosmico.skinswitch.config.ConfigVersionChecker;
import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.item.LoreRenderer;
import com.cesarcosmico.skinswitch.item.SkinSlotKeys;
import com.cesarcosmico.skinswitch.item.TokenFactory;
import com.cesarcosmico.skinswitch.listener.SkinSlotListener;
import com.cesarcosmico.skinswitch.placeholder.NoopPlaceholderResolver;
import com.cesarcosmico.skinswitch.placeholder.PlaceholderApiResolver;
import com.cesarcosmico.skinswitch.placeholder.PlaceholderResolver;
import com.cesarcosmico.skinswitch.service.SkinSlotService;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SkinSwitchPlugin extends JavaPlugin {

    private LangConfig langConfig;
    private PluginConfig pluginConfig;
    private SkinConfig skinConfig;
    private SkinSlotService skinSlotService;
    private TokenFactory skinTokenFactory;
    private TokenFactory tooltipTokenFactory;
    private PlaceholderResolver placeholderResolver;

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
        this.placeholderResolver = resolveDetected();

        SkinSlotKeys keys = new SkinSlotKeys(this);
        LoreRenderer loreRenderer = new LoreRenderer(
                this::getLangConfig, this::getSkinConfig, this::getPlaceholderResolver);
        this.skinSlotService = new SkinSlotService(keys, loreRenderer,
                this::getSkinConfig, this::getPluginConfig, this::getPlaceholderResolver);
        this.skinTokenFactory = new TokenFactory(keys.tokenSkin(),
                () -> getPluginConfig().getToken(), this::getSkinConfig, Material.NAME_TAG);
        this.tooltipTokenFactory = new TokenFactory(keys.tokenTooltip(),
                () -> getPluginConfig().getTooltipToken(), this::getSkinConfig, Material.PAPER);

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new SkinSlotListener(skinSlotService, skinTokenFactory, tooltipTokenFactory,
                        this::getLangConfig, this::getSkinConfig), this);

        getLogger().info("SkinSwitch enabled.");
    }

    private PlaceholderResolver resolveDetected() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI detected — placeholders will be resolved in skin name/lore.");
            return new PlaceholderApiResolver();
        }
        return new NoopPlaceholderResolver();
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
                this::getTooltipTokenFactory,
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
    public TokenFactory getSkinTokenFactory() { return skinTokenFactory; }
    public TokenFactory getTooltipTokenFactory() { return tooltipTokenFactory; }
    public PlaceholderResolver getPlaceholderResolver() { return placeholderResolver; }
}
