package com.cesarcosmico.switchskin.gui;

import com.cesarcosmico.switchskin.config.IconConfig;
import com.cesarcosmico.switchskin.config.MenuConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.item.IconFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SkinMenuGUI implements InventoryHolder {

    public sealed interface MenuAction
            permits MenuAction.SelectSkin, MenuAction.SelectVanilla,
                    MenuAction.Close, MenuAction.PrevPage, MenuAction.NextPage {
        record SelectSkin(String skinId) implements MenuAction {}
        record SelectVanilla() implements MenuAction {}
        record Close() implements MenuAction {}
        record PrevPage() implements MenuAction {}
        record NextPage() implements MenuAction {}
    }

    private final Inventory inventory;
    private final MenuAction[] actionBySlot;
    private final int page;
    private final int totalPages;
    private final int pageSize;

    public SkinMenuGUI(MenuConfig menuConfig, SkinConfig skinConfig,
                       List<String> skinIds, int activeIndex, int requestedPage) {
        final int size = menuConfig.getInventorySize();
        this.inventory = Bukkit.createInventory(this, size, menuConfig.getTitle());
        this.actionBySlot = new MenuAction[size];
        this.pageSize = Math.max(1, menuConfig.getSkinSlotPositions().size());
        this.totalPages = Math.max(1, (int) Math.ceil(skinIds.size() / (double) pageSize));
        this.page = Math.clamp(requestedPage, 0, totalPages - 1);

        populate(menuConfig, skinConfig, skinIds, activeIndex);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public MenuAction actionAt(int slot) {
        if (slot < 0 || slot >= actionBySlot.length) return null;
        return actionBySlot[slot];
    }

    public int getPage() { return page; }
    public int getTotalPages() { return totalPages; }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void populate(MenuConfig menu, SkinConfig skinConfig,
                          List<String> skinIds, int activeIndex) {
        fillDecoration(menu);
        fillSkinSlots(menu, skinConfig, skinIds, activeIndex);
        fillVanillaButton(menu, activeIndex);
        fillCloseButton(menu);
        fillPaginationButtons(menu);
    }

    private void fillDecoration(MenuConfig menu) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final char symbol = menu.getLayout().getSymbolAt(slot);
            final ItemStack icon = menu.getDecorativeIcons().get(symbol);
            if (icon != null) inventory.setItem(slot, icon.clone());
        }
    }

    private void fillSkinSlots(MenuConfig menu, SkinConfig skinConfig,
                               List<String> skinIds, int activeIndex) {
        final IconFactory factory = menu.getIconFactory();
        final int[] slots = sortedSlots(menu.getSkinSlotPositions());
        final int start = page * pageSize;

        for (int i = 0; i < slots.length; i++) {
            final int globalIndex = start + i;
            if (globalIndex >= skinIds.size()) {
                inventory.setItem(slots[i], null);
                continue;
            }
            final String skinId = skinIds.get(globalIndex);
            final SkinDefinition def = skinConfig.get(skinId).orElse(null);
            final boolean active = globalIndex == activeIndex;
            final IconConfig template = active ? menu.getSkinSlotActive() : menu.getSkinSlotInactive();
            final String displayName = def != null ? def.nameOrId() : skinId;
            final ItemStack item = factory.build(template,
                    Map.of("{skin}", displayName),
                    def != null ? def.itemModel() : null);
            inventory.setItem(slots[i], item);
            actionBySlot[slots[i]] = new MenuAction.SelectSkin(skinId);
        }
    }

    private void fillVanillaButton(MenuConfig menu, int activeIndex) {
        final IconConfig template = activeIndex < 0 ? menu.getVanillaActive() : menu.getVanillaInactive();
        final ItemStack item = menu.getIconFactory().build(template);
        for (int slot : menu.getVanillaPositions()) {
            inventory.setItem(slot, item.clone());
            actionBySlot[slot] = new MenuAction.SelectVanilla();
        }
    }

    private void fillCloseButton(MenuConfig menu) {
        final ItemStack item = menu.getIconFactory().build(menu.getCloseIcon());
        for (int slot : menu.getClosePositions()) {
            inventory.setItem(slot, item.clone());
            actionBySlot[slot] = new MenuAction.Close();
        }
    }

    private void fillPaginationButtons(MenuConfig menu) {
        if (page > 0) fillNav(menu, menu.getPrevPositions(), menu.getPrevIcon(), new MenuAction.PrevPage());
        if (page < totalPages - 1) fillNav(menu, menu.getNextPositions(), menu.getNextIcon(), new MenuAction.NextPage());
    }

    private void fillNav(MenuConfig menu, Set<Integer> positions, IconConfig icon, MenuAction action) {
        if (positions.isEmpty()) return;
        final ItemStack item = menu.getIconFactory().build(icon, pageInfoPlaceholders(), null);
        for (int slot : positions) {
            inventory.setItem(slot, item.clone());
            actionBySlot[slot] = action;
        }
    }

    private Map<String, String> pageInfoPlaceholders() {
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{page}", String.valueOf(page + 1));
        placeholders.put("{pages}", String.valueOf(totalPages));
        return placeholders;
    }

    private static int[] sortedSlots(Set<Integer> slots) {
        final int[] arr = slots.stream().mapToInt(Integer::intValue).toArray();
        Arrays.sort(arr);
        return arr;
    }
}
