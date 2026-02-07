package com.example.magicmod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record MagicProfileSnapshot(
        Set<String> learnedSpells,
        Map<Integer, String> keyBinds,
        int magicLevel,
        int magicExperience,
        int currentMana
) {
    public MagicProfileSnapshot {
        learnedSpells = new HashSet<>(learnedSpells);
        keyBinds = new HashMap<>(keyBinds);
        magicLevel = Math.max(1, magicLevel);
        magicExperience = Math.max(0, magicExperience);
        currentMana = Math.max(0, currentMana);
    }
}
