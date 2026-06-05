package com.cesarcosmico.switchskin.config;

import com.cesarcosmico.switchskin.items.CompiledItem;
import com.cesarcosmico.switchskin.items.ItemContext;
import com.cesarcosmico.switchskin.items.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class MenuConfig {

    public static final int CURRENT_VERSION = 1;

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final String DEFAULT_TITLE =
            "<white><gradient:#B4E488:#7DD031><b>Switch Skin</b></gradient></white>";
    private static final List<String> DEFAULT_LAYOUT = List.of(
            "XXXXXXXXX",
            "XSSSSSSSX",
            "PVXXXXXCN");

    private final Component title;
    private final LayoutParser layout;

    private final char skinSlotSymbol;
    private final CompiledItem skinSlotActive;
    private final CompiledItem skinSlotInactive;

    private final char vanillaSymbol;
    private final CompiledItem vanillaActive;
    private final CompiledItem vanillaInactive;

    private final char closeSymbol;
    private final CompiledItem closeIcon;

    private final char prevSymbol;
    private final CompiledItem prevIcon;

    private final char nextSymbol;
    private final CompiledItem nextIcon;

    private final char fillEmptySymbol;
    private final Map<Character, ItemStack> decorativeIcons;

    public MenuConfig(ConfigurationSection root, ItemFactory itemFactory, Logger logger) {
        final ConfigurationSection effective = root != null ? root : empty();
        this.title = MINI.deserialize(effective.getString("title", DEFAULT_TITLE));
        this.layout = new LayoutParser(
                effective.isList("layout") ? effective.getStringList("layout") : DEFAULT_LAYOUT, logger);

        final ConfigurationSection skinSection = effective.getConfigurationSection("skin-slot");
        this.skinSlotSymbol = symbol(skinSection, "S");
        this.skinSlotActive = itemFactory.compile(
                skinSection != null ? skinSection.getConfigurationSection("entry-active") : null);
        this.skinSlotInactive = itemFactory.compile(
                skinSection != null ? skinSection.getConfigurationSection("entry-inactive") : null);

        final ConfigurationSection vanillaSection = effective.getConfigurationSection("vanilla-button");
        this.vanillaSymbol = symbol(vanillaSection, "V");
        this.vanillaActive = itemFactory.compile(
                vanillaSection != null ? vanillaSection.getConfigurationSection("active") : null);
        this.vanillaInactive = itemFactory.compile(
                vanillaSection != null ? vanillaSection.getConfigurationSection("inactive") : null);

        final ConfigurationSection closeSection = effective.getConfigurationSection("close-button");
        this.closeSymbol = symbol(closeSection, "C");
        this.closeIcon = itemFactory.compile(closeSection);

        final ConfigurationSection prevSection = effective.getConfigurationSection("prev-button");
        this.prevSymbol = symbol(prevSection, "P");
        this.prevIcon = itemFactory.compile(prevSection);

        final ConfigurationSection nextSection = effective.getConfigurationSection("next-button");
        this.nextSymbol = symbol(nextSection, "N");
        this.nextIcon = itemFactory.compile(nextSection);

        this.decorativeIcons = parseDecorative(effective.getConfigurationSection("decorative-icons"), itemFactory);

        final String fillRaw = effective.getString("fill-empty", "X");
        this.fillEmptySymbol = fillRaw == null || fillRaw.isEmpty() ? '\0' : fillRaw.charAt(0);

        validateSymbols(logger);
    }

    private static ConfigurationSection empty() {
        return new org.bukkit.configuration.MemoryConfiguration();
    }

    private void validateSymbols(Logger logger) {
        final Set<Character> backed = new HashSet<>(decorativeIcons.keySet());
        backed.add(skinSlotSymbol);
        backed.add(vanillaSymbol);
        backed.add(closeSymbol);
        backed.add(prevSymbol);
        backed.add(nextSymbol);
        for (char symbol : layout.getSymbols()) {
            if (!backed.contains(symbol)) {
                logger.warning("menu.yml: layout symbol '" + symbol
                        + "' has no matching button or decorative-icon (its slots stay empty).");
            }
        }
    }

    private static char symbol(ConfigurationSection section, String fallback) {
        final String raw = section != null ? section.getString("symbol", fallback) : fallback;
        return raw.isEmpty() ? fallback.charAt(0) : raw.charAt(0);
    }

    private Map<Character, ItemStack> parseDecorative(ConfigurationSection section, ItemFactory itemFactory) {
        final Map<Character, ItemStack> result = new LinkedHashMap<>();
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            final ConfigurationSection icon = section.getConfigurationSection(key);
            if (icon == null) continue;
            final char symbol = icon.getString("symbol", "?").charAt(0);
            final ItemStack built = itemFactory.build(itemFactory.compile(icon), ItemContext.empty(), null, 1);
            result.put(symbol, built);
        }
        return result;
    }

    public Component getTitle() { return title; }
    public int getInventorySize() { return layout.getInventorySize(); }
    public LayoutParser getLayout() { return layout; }

    public char getSkinSlotSymbol() { return skinSlotSymbol; }
    public Set<Integer> getSkinSlotPositions() { return layout.getSlotsForSymbol(skinSlotSymbol); }
    public CompiledItem getSkinSlotActive() { return skinSlotActive; }
    public CompiledItem getSkinSlotInactive() { return skinSlotInactive; }

    public char getVanillaSymbol() { return vanillaSymbol; }
    public Set<Integer> getVanillaPositions() { return layout.getSlotsForSymbol(vanillaSymbol); }
    public CompiledItem getVanillaActive() { return vanillaActive; }
    public CompiledItem getVanillaInactive() { return vanillaInactive; }

    public char getCloseSymbol() { return closeSymbol; }
    public Set<Integer> getClosePositions() { return layout.getSlotsForSymbol(closeSymbol); }
    public CompiledItem getCloseIcon() { return closeIcon; }

    public char getPrevSymbol() { return prevSymbol; }
    public Set<Integer> getPrevPositions() { return layout.getSlotsForSymbol(prevSymbol); }
    public CompiledItem getPrevIcon() { return prevIcon; }

    public char getNextSymbol() { return nextSymbol; }
    public Set<Integer> getNextPositions() { return layout.getSlotsForSymbol(nextSymbol); }
    public CompiledItem getNextIcon() { return nextIcon; }

    public Map<Character, ItemStack> getDecorativeIcons() { return decorativeIcons; }

    public ItemStack getEmptyFillIcon() {
        if (fillEmptySymbol == '\0') return null;
        return decorativeIcons.get(fillEmptySymbol);
    }
}
