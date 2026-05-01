package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.item.IconFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class MenuConfig {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String DEFAULT_TITLE =
            "<white><gradient:#B4E488:#7DD031><b>Switch Skin</b></gradient></white>";

    private final Component title;
    private final LayoutParser layout;
    private final IconFactory iconFactory;

    private final char skinSlotSymbol;
    private final IconConfig skinSlotActive;
    private final IconConfig skinSlotInactive;

    private final char vanillaSymbol;
    private final IconConfig vanillaActive;
    private final IconConfig vanillaInactive;

    private final char closeSymbol;
    private final IconConfig closeIcon;

    private final Map<Character, ItemStack> decorativeIcons;

    public MenuConfig(ConfigurationSection root, Logger logger) {
        this.iconFactory = new IconFactory(logger);

        if (root == null) {
            this.title = MINI.deserialize(DEFAULT_TITLE);
            this.layout = new LayoutParser(java.util.List.of(
                    "XXXXXXXXX",
                    "XSSSSSSSX",
                    "XVXXXXXCX"), logger);
            this.skinSlotSymbol = 'S';
            this.vanillaSymbol = 'V';
            this.closeSymbol = 'C';
            this.skinSlotActive = new IconConfig("NAME_TAG", "<gradient:#B4E488:#7DD031><b>{skin}</b></gradient>", java.util.List.of(), null);
            this.skinSlotInactive = new IconConfig("NAME_TAG", "<white>{skin}</white>", java.util.List.of(), null);
            this.vanillaActive = new IconConfig("BARRIER", "<red><b>Vanilla (active)</b></red>", java.util.List.of(), null);
            this.vanillaInactive = new IconConfig("BARRIER", "<red>Vanilla</red>", java.util.List.of(), null);
            this.closeIcon = new IconConfig("OAK_DOOR", "<red>Close</red>", java.util.List.of(), null);
            this.decorativeIcons = new HashMap<>();
            return;
        }

        this.title = MINI.deserialize(root.getString("title", DEFAULT_TITLE));
        this.layout = new LayoutParser(root.getStringList("layout"), logger);

        final ConfigurationSection skinSection = root.getConfigurationSection("skin-slot");
        this.skinSlotSymbol = symbol(skinSection, "S");
        this.skinSlotActive = iconFactory.parse(
                skinSection != null ? skinSection.getConfigurationSection("entry-active") : null, "NAME_TAG");
        this.skinSlotInactive = iconFactory.parse(
                skinSection != null ? skinSection.getConfigurationSection("entry-inactive") : null, "NAME_TAG");

        final ConfigurationSection vanillaSection = root.getConfigurationSection("vanilla-button");
        this.vanillaSymbol = symbol(vanillaSection, "V");
        this.vanillaActive = iconFactory.parse(
                vanillaSection != null ? vanillaSection.getConfigurationSection("active") : null, "BARRIER");
        this.vanillaInactive = iconFactory.parse(
                vanillaSection != null ? vanillaSection.getConfigurationSection("inactive") : null, "BARRIER");

        final ConfigurationSection closeSection = root.getConfigurationSection("close-button");
        this.closeSymbol = symbol(closeSection, "C");
        this.closeIcon = iconFactory.parse(closeSection, "OAK_DOOR");

        this.decorativeIcons = parseDecorative(root.getConfigurationSection("decorative-icons"));
    }

    private static char symbol(ConfigurationSection section, String fallback) {
        final String raw = section != null ? section.getString("symbol", fallback) : fallback;
        return raw.isEmpty() ? fallback.charAt(0) : raw.charAt(0);
    }

    private Map<Character, ItemStack> parseDecorative(ConfigurationSection section) {
        final Map<Character, ItemStack> result = new LinkedHashMap<>();
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            final ConfigurationSection icon = section.getConfigurationSection(key);
            if (icon == null) continue;
            final char symbol = icon.getString("symbol", "?").charAt(0);
            final IconConfig parsed = iconFactory.parse(icon, "AIR");
            result.put(symbol, iconFactory.build(parsed));
        }
        return result;
    }

    public Component getTitle() { return title; }
    public int getInventorySize() { return layout.getInventorySize(); }
    public LayoutParser getLayout() { return layout; }
    public IconFactory getIconFactory() { return iconFactory; }

    public char getSkinSlotSymbol() { return skinSlotSymbol; }
    public Set<Integer> getSkinSlotPositions() { return layout.getSlotsForSymbol(skinSlotSymbol); }
    public IconConfig getSkinSlotActive() { return skinSlotActive; }
    public IconConfig getSkinSlotInactive() { return skinSlotInactive; }

    public char getVanillaSymbol() { return vanillaSymbol; }
    public Set<Integer> getVanillaPositions() { return layout.getSlotsForSymbol(vanillaSymbol); }
    public IconConfig getVanillaActive() { return vanillaActive; }
    public IconConfig getVanillaInactive() { return vanillaInactive; }

    public char getCloseSymbol() { return closeSymbol; }
    public Set<Integer> getClosePositions() { return layout.getSlotsForSymbol(closeSymbol); }
    public IconConfig getCloseIcon() { return closeIcon; }

    public Map<Character, ItemStack> getDecorativeIcons() { return decorativeIcons; }
}
