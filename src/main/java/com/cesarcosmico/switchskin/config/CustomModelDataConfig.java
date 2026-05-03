package com.cesarcosmico.switchskin.config;

import java.util.List;

public record CustomModelDataConfig(
        List<Float> floats,
        List<Boolean> flags,
        List<String> strings,
        List<Integer> colors
) {
    public CustomModelDataConfig {
        floats = floats == null ? List.of() : List.copyOf(floats);
        flags = flags == null ? List.of() : List.copyOf(flags);
        strings = strings == null ? List.of() : List.copyOf(strings);
        colors = colors == null ? List.of() : List.copyOf(colors);
    }

    public boolean isEmpty() {
        return floats.isEmpty() && flags.isEmpty() && strings.isEmpty() && colors.isEmpty();
    }
}
