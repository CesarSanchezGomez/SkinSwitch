package com.cesarcosmico.switchskin.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class LayoutParser {

    private final String[] rows;
    private final int inventorySize;
    private final Map<Character, Set<Integer>> slotsBySymbol;

    public LayoutParser(List<String> layoutRows, Logger logger) {
        this.rows = layoutRows.toArray(new String[0]);
        this.inventorySize = rows.length * 9;

        if (inventorySize < 9 || inventorySize > 54 || inventorySize % 9 != 0) {
            logger.warning("Invalid layout size: " + rows.length
                    + " rows (" + inventorySize + " slots). Must be 1-6 rows.");
        }

        this.slotsBySymbol = new HashMap<>();
        for (int row = 0; row < rows.length; row++) {
            final String line = rows[row];
            for (int col = 0; col < Math.min(line.length(), 9); col++) {
                final int slot = row * 9 + col;
                final char symbol = line.charAt(col);
                slotsBySymbol.computeIfAbsent(symbol, k -> new LinkedHashSet<>()).add(slot);
            }
        }
    }

    public int getInventorySize() {
        return inventorySize;
    }

    public Set<Integer> getSlotsForSymbol(char symbol) {
        return slotsBySymbol.getOrDefault(symbol, Collections.emptySet());
    }

    public char getSymbolAt(int slot) {
        final int row = slot / 9;
        final int col = slot % 9;
        if (row < rows.length && col < rows[row].length()) {
            return rows[row].charAt(col);
        }
        return ' ';
    }
}
