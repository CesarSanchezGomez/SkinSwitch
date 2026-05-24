package com.cesarcosmico.switchskin.items;

import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import com.cesarcosmico.switchskin.config.TokenVisualConfig;
import com.cesarcosmico.switchskin.items.value.ComponentValue;
import com.cesarcosmico.switchskin.items.value.LiteralValue;
import com.cesarcosmico.switchskin.items.value.ValueCompiler;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.saicone.rtag.RtagItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Builds tokens and menu icons from a compiled component AST, applying each
 * vanilla data component through RTAG ({@link ComponentDispatcher}). The only
 * components handled outside the generic dispatch are {@code material} (defines
 * the stack) and {@code profile} (applied via the Paper profile API, since the
 * config format is {@code textures}/name rather than the vanilla component shape).
 */
public final class ItemFactory {

    private final Logger logger;
    private final ComponentDispatcher dispatcher;

    public ItemFactory(Logger logger) {
        this.logger = logger;
        this.dispatcher = new ComponentDispatcher(logger);
    }

    public Map<String, ComponentValue> compile(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        return ValueCompiler.compileTopLevel(section);
    }

    /** Build an item from a compiled component map. {@code materialOverride} wins over the AST material. */
    public ItemStack build(Map<String, ComponentValue> components, ItemContext context,
                           @Nullable Material materialOverride, int amount) {
        return assemble(components, context, materialOverride, amount, null, false, null);
    }

    public ItemStack buildSkinToken(Map<String, ComponentValue> base, ItemContext context,
                                    int amount, String skinId, @Nullable TokenVisualConfig override) {
        return assemble(base, context, null, amount, skinId, true, override);
    }

    public ItemStack buildTooltipToken(Map<String, ComponentValue> base, ItemContext context,
                                       int amount, String skinId, @Nullable TokenVisualConfig override) {
        return assemble(base, context, null, amount, skinId, false, override);
    }

    private ItemStack assemble(Map<String, ComponentValue> components, ItemContext context,
                               @Nullable Material materialOverride, int amount,
                               @Nullable String tokenId, boolean skinToken,
                               @Nullable TokenVisualConfig override) {
        Material material = materialOverride != null ? materialOverride : resolveMaterial(components);
        if (components.containsKey("profile") && material != Material.PLAYER_HEAD) {
            material = Material.PLAYER_HEAD;
        }

        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        applyProfile(stack, components, context);

        RtagItem item = new RtagItem(stack);
        dispatcher.apply(item, components, context);
        applyTokenOverride(item, override);
        if (tokenId != null) {
            if (skinToken) {
                PluginItemTag.writeSkinToken(item, tokenId);
            } else {
                PluginItemTag.writeTooltipToken(item, tokenId);
            }
        }
        return item.load();
    }

    private void applyTokenOverride(RtagItem item, @Nullable TokenVisualConfig override) {
        if (override == null) {
            return;
        }
        if (override.itemModel() != null) {
            item.setComponent("minecraft:item_model", override.itemModel().toString());
        }
        CustomModelDataConfig cmd = override.customModelData();
        if (cmd != null && !cmd.isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<>();
            if (!cmd.floats().isEmpty()) map.put("floats", cmd.floats());
            if (!cmd.flags().isEmpty()) map.put("flags", cmd.flags());
            if (!cmd.strings().isEmpty()) map.put("strings", cmd.strings());
            if (!cmd.colors().isEmpty()) map.put("colors", cmd.colors());
            item.setComponent("minecraft:custom_model_data", map);
        }
    }

    private Material resolveMaterial(Map<String, ComponentValue> components) {
        if (components.get("material") instanceof LiteralValue literal
                && literal.value() instanceof String name) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
            logger.warning("Unknown material: " + name + ", using STONE");
        }
        return Material.STONE;
    }

    private void applyProfile(ItemStack stack, Map<String, ComponentValue> components, ItemContext context) {
        ComponentValue value = components.get("profile");
        if (value == null || !(stack.getItemMeta() instanceof SkullMeta meta)) {
            return;
        }
        Object resolved = value.resolve(context);
        try {
            if (resolved instanceof Map<?, ?> map) {
                Object textures = map.get("textures");
                if (textures instanceof String encoded && !encoded.isEmpty()) {
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                    profile.clearProperties();
                    profile.setProperty(new ProfileProperty("textures", encoded));
                    meta.setPlayerProfile(profile);
                    stack.setItemMeta(meta);
                }
            } else if (resolved instanceof String name && !name.isEmpty()) {
                meta.setPlayerProfile(Bukkit.createProfile(name));
                stack.setItemMeta(meta);
            }
        } catch (RuntimeException invalidProfile) {
            logger.warning("Invalid profile: " + invalidProfile.getMessage());
        }
    }
}
