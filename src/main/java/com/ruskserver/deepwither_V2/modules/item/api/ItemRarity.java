package com.ruskserver.deepwither_V2.modules.item.api;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public enum ItemRarity {
    COMMON("COMMON", NamedTextColor.WHITE),
    UNCOMMON("UNCOMMON", NamedTextColor.GREEN),
    RARE("RARE", NamedTextColor.BLUE),
    EPIC("EPIC", NamedTextColor.DARK_PURPLE),
    LEGENDARY("LEGENDARY", NamedTextColor.GOLD);

    private final String displayName;
    private final TextColor color;

    ItemRarity(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TextColor getColor() {
        return color;
    }
}
