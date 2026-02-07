package com.example.magicmod.fabric;

import java.util.ArrayList;
import java.util.List;

public record MagicHudState(int level, int mana, int maxMana, List<String> boundSlots) {
    public MagicHudState {
        boundSlots = new ArrayList<>(boundSlots);
    }

    public static MagicHudState empty() {
        return new MagicHudState(1, 0, 100, List.of("", "", "", "", "", "", "", "", ""));
    }
}
