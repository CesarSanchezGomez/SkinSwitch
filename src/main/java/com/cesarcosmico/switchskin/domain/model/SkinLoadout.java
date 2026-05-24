package com.cesarcosmico.switchskin.domain.model;

import com.cesarcosmico.switchskin.domain.id.SkinId;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Immutable, Bukkit-free skin state. Transitions return (next loadout, outcome)
// with no side effects; VANILLA_INDEX = the original look.
public record SkinLoadout(List<SkinId> slots, int activeIndex, Set<SkinId> tooltips) {

    public static final int VANILLA_INDEX = -1;

    public SkinLoadout {
        slots = List.copyOf(slots);
        tooltips = Set.copyOf(tooltips);
    }

    public static SkinLoadout empty() {
        return new SkinLoadout(List.of(), 0, Set.of());
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    public int size() {
        return slots.size();
    }

    public boolean isVanillaActive() {
        return activeIndex == VANILLA_INDEX;
    }

    public boolean contains(SkinId id) {
        return slots.contains(id);
    }

    public boolean hasTooltip(SkinId id) {
        return tooltips.contains(id);
    }

    public Optional<SkinId> activeSkin() {
        if (activeIndex < 0 || activeIndex >= slots.size()) {
            return Optional.empty();
        }
        return Optional.of(slots.get(activeIndex));
    }

    public enum AddOutcome { ADDED, FULL, DUPLICATE }

    public enum RemoveOutcome { REMOVED, INVALID_INDEX }

    public enum CycleOutcome { CYCLED, SINGLE_SLOT, EMPTY }

    public enum SelectOutcome { SELECTED, INVALID_INDEX, ALREADY_ACTIVE, EMPTY }

    public enum VanillaOutcome { APPLIED, ALREADY_VANILLA, EMPTY }

    public enum TooltipAddOutcome { ADDED, NO_SLOT, ALREADY }

    public enum TooltipRemoveOutcome { REMOVED, NONE }

    public record Change<O>(SkinLoadout loadout, O outcome) {}

    public Change<AddOutcome> add(SkinId id, int maxSlots) {
        if (slots.size() >= maxSlots) {
            return new Change<>(this, AddOutcome.FULL);
        }
        if (slots.contains(id)) {
            return new Change<>(this, AddOutcome.DUPLICATE);
        }
        List<SkinId> next = new ArrayList<>(slots);
        next.add(id);
        int index = isEmpty() ? 0 : activeIndex;
        return new Change<>(new SkinLoadout(next, index, tooltips), AddOutcome.ADDED);
    }

    public Change<RemoveOutcome> remove(int index) {
        if (index < 0 || index >= slots.size()) {
            return new Change<>(this, RemoveOutcome.INVALID_INDEX);
        }
        List<SkinId> next = new ArrayList<>(slots);
        SkinId removed = next.remove(index);
        if (next.isEmpty()) {
            return new Change<>(empty(), RemoveOutcome.REMOVED);
        }
        Set<SkinId> nextTooltips = new LinkedHashSet<>(tooltips);
        nextTooltips.remove(removed);
        int active = activeIndex >= next.size() ? 0 : activeIndex;
        return new Change<>(new SkinLoadout(next, active, nextTooltips), RemoveOutcome.REMOVED);
    }

    public Change<CycleOutcome> cycleNext() {
        if (slots.isEmpty()) {
            return new Change<>(this, CycleOutcome.EMPTY);
        }
        int next = isVanillaActive() ? 0 : (activeIndex + 1) % slots.size();
        if (next == activeIndex) {
            return new Change<>(this, CycleOutcome.SINGLE_SLOT);
        }
        return new Change<>(new SkinLoadout(slots, next, tooltips), CycleOutcome.CYCLED);
    }

    public Change<SelectOutcome> select(int index) {
        if (slots.isEmpty()) {
            return new Change<>(this, SelectOutcome.EMPTY);
        }
        if (index < 0 || index >= slots.size()) {
            return new Change<>(this, SelectOutcome.INVALID_INDEX);
        }
        if (activeIndex == index) {
            return new Change<>(this, SelectOutcome.ALREADY_ACTIVE);
        }
        return new Change<>(new SkinLoadout(slots, index, tooltips), SelectOutcome.SELECTED);
    }

    public Change<VanillaOutcome> selectVanilla() {
        if (slots.isEmpty()) {
            return new Change<>(this, VanillaOutcome.EMPTY);
        }
        if (isVanillaActive()) {
            return new Change<>(this, VanillaOutcome.ALREADY_VANILLA);
        }
        return new Change<>(new SkinLoadout(slots, VANILLA_INDEX, tooltips), VanillaOutcome.APPLIED);
    }

    public Change<TooltipAddOutcome> addTooltip(SkinId id) {
        if (!slots.contains(id)) {
            return new Change<>(this, TooltipAddOutcome.NO_SLOT);
        }
        if (tooltips.contains(id)) {
            return new Change<>(this, TooltipAddOutcome.ALREADY);
        }
        Set<SkinId> next = new LinkedHashSet<>(tooltips);
        next.add(id);
        return new Change<>(new SkinLoadout(slots, activeIndex, next), TooltipAddOutcome.ADDED);
    }

    public Change<TooltipRemoveOutcome> removeTooltip(SkinId id) {
        if (!tooltips.contains(id)) {
            return new Change<>(this, TooltipRemoveOutcome.NONE);
        }
        Set<SkinId> next = new LinkedHashSet<>(tooltips);
        next.remove(id);
        return new Change<>(new SkinLoadout(slots, activeIndex, next), TooltipRemoveOutcome.REMOVED);
    }
}
