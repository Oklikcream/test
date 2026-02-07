package com.example.magicmod.fabric;

import java.util.ArrayList;
import java.util.List;

public record MagicUiState(int level, int mana, int maxMana, List<String> learnedSpells, List<String> boundSlots) {
    public MagicUiState {
        learnedSpells = new ArrayList<>(learnedSpells);
        boundSlots = new ArrayList<>(boundSlots);
    }
}
