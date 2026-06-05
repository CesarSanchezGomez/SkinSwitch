package com.cesarcosmico.switchskin.items;

import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import com.cesarcosmico.switchskin.config.TokenVisualConfig;
import com.cesarcosmico.switchskin.items.value.ValueCompiler;
import com.saicone.rtag.RtagItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/** Builds tokens and menu icons, applying each vanilla data component through RTAG. */
public final class ItemFactory {

    private final Logger logger;
    private final ComponentDispatcher dispatcher;

    public ItemFactory(Logger logger) {
        this.logger = logger;
        this.dispatcher = new ComponentDispatcher(logger);
    }

    public CompiledItem compile(@Nullable ConfigurationSection itemSection) {
        if (itemSection == null) {
            return CompiledItem.EMPTY;
        }
        return new CompiledItem(
                resolveMaterial(itemSection.getString("material")),
                ValueCompiler.compileComponents(itemSection.getConfigurationSection("components")));
    }

    public ItemStack build(CompiledItem item, ItemContext context,
                           @Nullable Material materialOverride, int amount) {
        return assemble(item, context, materialOverride, amount, null, false, null);
    }

    public ItemStack buildSkinToken(CompiledItem item, ItemContext context,
                                    int amount, String skinId, @Nullable TokenVisualConfig override) {
        return assemble(item, context, null, amount, skinId, true, override);
    }

    public ItemStack buildTooltipToken(CompiledItem item, ItemContext context,
                                       int amount, String skinId, @Nullable TokenVisualConfig override) {
        return assemble(item, context, null, amount, skinId, false, override);
    }

    private ItemStack assemble(CompiledItem compiled, ItemContext context,
                               @Nullable Material materialOverride, int amount,
                               @Nullable String tokenId, boolean skinToken,
                               @Nullable TokenVisualConfig override) {
        Material material = materialOverride != null ? materialOverride
                : compiled.material() != null ? compiled.material() : Material.STONE;
        ItemStack stack = new ItemStack(material, Math.max(1, amount));
        if (material.isAir()) {
            return stack;
        }

        RtagItem item = new RtagItem(stack);
        dispatcher.apply(item, compiled.components(), context);
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

    @Nullable
    private Material resolveMaterial(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(name);
        if (material != null) {
            return material;
        }
        logger.warning("Unknown material: " + name + ", using STONE");
        return Material.STONE;
    }
}
