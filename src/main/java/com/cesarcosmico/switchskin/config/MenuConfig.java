package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.item.IconFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class MenuConfig {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String DEFAULT_TITLE =
            "<white><gradient:#B4E488:#7DD031><b>Switch Skin</b></gradient></white>";
    private static final List<String> DEFAULT_LAYOUT = List.of(
            "XXXXXXXXX",
            "XSSSSSSSX",
            "PVXXXXXCN");

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

    private final char prevSymbol;
    private final IconConfig prevIcon;

    private final char nextSymbol;
    private final IconConfig nextIcon;

    private final Map<Character, ItemStack> decorativeIcons;

    public MenuConfig(ConfigurationSection root, Logger logger) {
        this.iconFactory = new IconFactory(logger);

        final ConfigurationSection effective = root != null ? root : empty();
        this.title = MINI.deserialize(effective.getString("title", DEFAULT_TITLE));
        this.layout = new LayoutParser(
                effective.isList("layout") ? effective.getStringList("layout") : DEFAULT_LAYOUT, logger);

        final ConfigurationSection skinSection = effective.getConfigurationSection("skin-slot");
        this.skinSlotSymbol = symbol(skinSection, "S");
        this.skinSlotActive = iconFactory.parse(
                skinSection != null ? skinSection.getConfigurationSection("entry-active") : null, "NAME_TAG");
        this.skinSlotInactive = iconFactory.parse(
                skinSection != null ? skinSection.getConfigurationSection("entry-inactive") : null, "NAME_TAG");

        final ConfigurationSection vanillaSection = effective.getConfigurationSection("vanilla-button");
        this.vanillaSymbol = symbol(vanillaSection, "V");
        this.vanillaActive = iconFactory.parse(
                vanillaSection != null ? vanillaSection.getConfigurationSection("active") : null, "BARRIER");
        this.vanillaInactive = iconFactory.parse(
                vanillaSection != null ? vanillaSection.getConfigurationSection("inactive") : null, "BARRIER");

        final ConfigurationSection closeSection = effective.getConfigurationSection("close-button");
        this.closeSymbol = symbol(closeSection, "C");
        this.closeIcon = iconFactory.parse(closeSection, "OAK_DOOR");

        final ConfigurationSection prevSection = effective.getConfigurationSection("prev-button");
        this.prevSymbol = symbol(prevSection, "P");
        this.prevIcon = iconFactory.parse(prevSection, "ARROW");

        final ConfigurationSection nextSection = effective.getConfigurationSection("next-button");
        this.nextSymbol = symbol(nextSection, "N");
        this.nextIcon = iconFactory.parse(nextSection, "ARROW");

        this.decorativeIcons = parseDecorative(effective.getConfigurationSection("decorative-icons"));
    }

    private static ConfigurationSection empty() {
        return new org.bukkit.configuration.MemoryConfiguration();
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

    public char getPrevSymbol() { return prevSymbol; }
    public Set<Integer> getPrevPositions() { return layout.getSlotsForSymbol(prevSymbol); }
    public IconConfig getPrevIcon() { return prevIcon; }

    public char getNextSymbol() { return nextSymbol; }
    public Set<Integer> getNextPositions() { return layout.getSlotsForSymbol(nextSymbol); }
    public IconConfig getNextIcon() { return nextIcon; }

    public Map<Character, ItemStack> getDecorativeIcons() { return decorativeIcons; }
}
