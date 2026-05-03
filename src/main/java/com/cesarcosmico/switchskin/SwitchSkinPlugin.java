package com.cesarcosmico.switchskin;

import com.cesarcosmico.switchskin.command.CommandManager;
import com.cesarcosmico.switchskin.config.ConfigVersionChecker;
import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.item.ItemFactory;
import com.cesarcosmico.switchskin.item.LoreRenderer;
import com.cesarcosmico.switchskin.item.SkinSlotKeys;
import com.cesarcosmico.switchskin.item.TokenFactory;
import com.cesarcosmico.switchskin.listener.SkinMenuListener;
import com.cesarcosmico.switchskin.listener.SkinTokenListener;
import com.cesarcosmico.switchskin.placeholder.NoopPlaceholderResolver;
import com.cesarcosmico.switchskin.placeholder.PlaceholderApiResolver;
import com.cesarcosmico.switchskin.placeholder.PlaceholderResolver;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SwitchSkinPlugin extends JavaPlugin {

    private ItemFactory itemFactory;
    private LangConfig langConfig;
    private PluginConfig pluginConfig;
    private SkinConfig skinConfig;
    private SkinSlotService skinSlotService;
    private TokenFactory skinTokenFactory;
    private TokenFactory tooltipTokenFactory;
    private PlaceholderResolver placeholderResolver;
    private SwitchAnnouncer switchAnnouncer;

    @Override
    public void onEnable() {
        this.itemFactory = new ItemFactory(getLogger());
        loadConfigurations();

        this.placeholderResolver = resolvePlaceholderBackend();
        this.switchAnnouncer = new SwitchAnnouncer(this::getLangConfig, this::getPluginConfig);

        final SkinSlotKeys keys = new SkinSlotKeys(this);
        final LoreRenderer loreRenderer = new LoreRenderer(
                this::getLangConfig, this::getSkinConfig, this::getPlaceholderResolver);
        this.skinSlotService = new SkinSlotService(keys, loreRenderer,
                this::getSkinConfig, this::getPluginConfig, this::getPlaceholderResolver);
        this.skinTokenFactory = new TokenFactory(keys.tokenSkin(),
                () -> getPluginConfig().getToken(), this::getSkinConfig, itemFactory,
                SkinDefinition::tokenSkin);
        this.tooltipTokenFactory = new TokenFactory(keys.tokenTooltip(),
                () -> getPluginConfig().getTooltipToken(), this::getSkinConfig, itemFactory,
                SkinDefinition::tokenTooltip);

        registerCommands();
        registerListeners();

        getLogger().info("SwitchSkin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SwitchSkin disabled.");
    }

    public void reload() {
        loadConfigurations();
    }

    private void loadConfigurations() {
        saveDefaultConfig();
        reloadConfig();
        ConfigVersionChecker.check(getConfig(), "config.yml",
                PluginConfig.CURRENT_VERSION, this, getLogger());

        saveDefaultIfMissing("skins.yml");
        final FileConfiguration skinsYml = YamlConfiguration.loadConfiguration(
                new File(getDataFolder(), "skins.yml"));
        ConfigVersionChecker.check(skinsYml, "skins.yml",
                SkinConfig.CURRENT_VERSION, this, getLogger(), "skins");

        if (this.langConfig == null) {
            this.langConfig = new LangConfig(this);
        } else {
            this.langConfig.load();
        }
        this.pluginConfig = new PluginConfig(getConfig(), itemFactory, getLogger());
        this.skinConfig = new SkinConfig(skinsYml, getLogger());
    }

    private PlaceholderResolver resolvePlaceholderBackend() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI detected — placeholders will be resolved in skin name/lore.");
            return new PlaceholderApiResolver();
        }
        return new NoopPlaceholderResolver();
    }

    private void registerCommands() {
        final CommandManager commandManager = new CommandManager(
                this::getLangConfig,
                this::getSkinConfig,
                this::getPluginConfig,
                this::getSkinSlotService,
                this::getSkinTokenFactory,
                this::getTooltipTokenFactory,
                this::getSwitchAnnouncer,
                this::reload
        );

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(commandManager.createCommand());
            event.registrar().register(commandManager.createAliasCommand());
        });
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new SkinTokenListener(skinSlotService, skinTokenFactory, tooltipTokenFactory,
                        this::getLangConfig, this::getSkinConfig, this::getSwitchAnnouncer), this);
        getServer().getPluginManager().registerEvents(
                new SkinMenuListener(this, this::getLangConfig, this::getSkinConfig,
                        this::getPluginConfig, this::getSkinSlotService,
                        this::getSwitchAnnouncer), this);
    }

    private void saveDefaultIfMissing(String name) {
        final File file = new File(getDataFolder(), name);
        if (!file.exists()) {
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
    public SwitchAnnouncer getSwitchAnnouncer() { return switchAnnouncer; }
}
