package com.cesarcosmico.switchskin.config;

import java.util.List;

public record TooltipDisplayConfig(boolean hideTooltip, List<String> hiddenComponents) {
    public TooltipDisplayConfig {
        hiddenComponents = hiddenComponents == null ? List.of() : List.copyOf(hiddenComponents);
    }
}
