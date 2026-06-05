package com.cesarcosmico.switchskin;

import com.cesarcosmico.switchskin.adapter.placeholderapi.NoopPlaceholderResolver;
import com.cesarcosmico.switchskin.adapter.placeholderapi.PlaceholderApiResolver;
import com.cesarcosmico.switchskin.adapter.placeholderapi.PlaceholderResolver;
import com.cesarcosmico.switchskin.command.CommandManager;
import com.cesarcosmico.switchskin.config.ConfigLoader;
import com.cesarcosmico.switchskin.config.ConfigVersionChecker;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI;
import com.cesarcosmico.switchskin.items.ItemFactory;
import com.cesarcosmico.switchskin.items.LoreRenderer;
import com.cesarcosmico.switchskin.items.SkinSlotKeys;
import com.cesarcosmico.switchskin.listener.SkinMenuListener;
import com.cesarcosmico.switchskin.listener.SkinTokenListener;
import com.cesarcosmico.switchskin.service.SkinAppearanceRenderer;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SkinStateCodec;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.cesarcosmico.switchskin.service.TooltipService;
import com.cesarcosmico.switchskin.text.MessageManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public final class SwitchSkinPlugin extends JavaPlugin {

    private ConfigLoader configLoader;
    private MessageManager messageManager;
    private SkinConfig skinConfig;
    private PluginConfig pluginConfig;
    private ItemFactory itemFactory;
    private PlaceholderResolver placeholderResolver;
    private SkinSlotKeys keys;
    private SkinStateCodec codec;
    private LoreRenderer loreRenderer;
    private SkinAppearanceRenderer appearanceRenderer;
    private SwitchAnnouncer switchAnnouncer;
    private SkinSlotService skinSlotService;
    private TooltipService tooltipService;

    @Override
    public void onEnable() {
        loadConfigs();
        registerIntegrations();
        bootstrapServices();
        registerListeners();
        registerCommands();

        getLogger().info("SwitchSkin enabled.");
    }

    @Override
    public void onDisable() {
        for (HumanEntity viewer : List.copyOf(getServer().getOnlinePlayers())) {
            if (viewer.getOpenInventory().getTopInventory().getHolder() instanceof SkinMenuGUI) {
                viewer.closeInventory();
            }
        }
        HandlerList.unregisterAll(this);
        getLogger().info("SwitchSkin disabled.");
    }

    public void reload() {
        try {
            reloadConfig();
            configLoader.load();
            pluginConfig.load(getConfig());
            skinConfig.load(loadSkins());
            messageManager.reload();
            checkConfigVersions();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during reload", e);
        }
    }

    private void loadConfigs() {
        saveDefaultConfig();
        reloadConfig();

        configLoader = new ConfigLoader(this);
        configLoader.load();

        messageManager = new MessageManager(this, configLoader);
        messageManager.reload();

        itemFactory = new ItemFactory(getLogger());

        skinConfig = new SkinConfig(loadSkins(), getLogger());
        pluginConfig = new PluginConfig(getConfig(), itemFactory, getLogger());

        checkConfigVersions();
    }

    private void registerIntegrations() {
        placeholderResolver = resolvePlaceholderBackend();
    }

    private void bootstrapServices() {
        keys = new SkinSlotKeys(this);
        codec = new SkinStateCodec(keys);
        loreRenderer = new LoreRenderer(messageManager, skinConfig, placeholderResolver);
        appearanceRenderer = new SkinAppearanceRenderer(codec, loreRenderer, skinConfig,
                placeholderResolver, getLogger());
        switchAnnouncer = new SwitchAnnouncer(messageManager, pluginConfig);
        tooltipService = new TooltipService(codec, appearanceRenderer, skinConfig);
        skinSlotService = new SkinSlotService(codec, appearanceRenderer, skinConfig, pluginConfig, tooltipService);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new SkinTokenListener(skinSlotService, messageManager, skinConfig, switchAnnouncer), this);
        getServer().getPluginManager().registerEvents(
                new SkinMenuListener(this, messageManager, skinConfig, pluginConfig, skinSlotService,
                        switchAnnouncer, itemFactory, appearanceRenderer), this);
    }

    private void registerCommands() {
        final CommandManager commandManager = new CommandManager(messageManager, skinConfig, pluginConfig,
                skinSlotService, switchAnnouncer, itemFactory, appearanceRenderer, this::reload);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(commandManager.createCommand());
            event.registrar().register(commandManager.createAliasCommand());
        });
    }

    private PlaceholderResolver resolvePlaceholderBackend() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().info("PlaceholderAPI detected — placeholders will be resolved in skin name/lore.");
            return new PlaceholderApiResolver();
        }
        return new NoopPlaceholderResolver();
    }

    private YamlConfiguration loadSkins() {
        final File file = new File(getDataFolder(), "skins.yml");
        if (!file.exists()) {
            saveResource("skins.yml", false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void checkConfigVersions() {
        ConfigVersionChecker.check(getConfig(), "config.yml",
                PluginConfig.CURRENT_VERSION, this, getLogger());
    }
}
