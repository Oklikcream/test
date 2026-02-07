package com.example.magicmod;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerMagicProfile {
    private static final int BASE_MANA = 100;
    private static final int MANA_PER_LEVEL = 25;
    private static final int BASE_EXP_TO_LEVEL = 100;

    private final Set<String> learnedSpells = new HashSet<>();
    private final Map<Integer, String> keyBinds = new HashMap<>();
    private int magicLevel = 1;
    private int magicExperience = 0;
    private int currentMana = maxMana();

    public boolean learnSpell(String spellId) {
        return learnedSpells.add(spellId);
    }

    public boolean hasSpell(String spellId) {
        return learnedSpells.contains(spellId);
    }

    public Set<String> learnedSpells() {
        return Collections.unmodifiableSet(learnedSpells);
    }

    public int magicLevel() {
        return magicLevel;
    }

    public int maxMana() {
        return BASE_MANA + (magicLevel - 1) * MANA_PER_LEVEL;
    }

    public int currentMana() {
        return currentMana;
    }

    public void regenerateMana(int amount) {
        currentMana = Math.min(maxMana(), currentMana + Math.max(0, amount));
    }

    public boolean spendMana(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (currentMana < amount) {
            return false;
        }
        currentMana -= amount;
        return true;
    }

    public void grantExperience(int amount) {
        if (amount <= 0) {
            return;
        }
        magicExperience += amount;
        while (magicExperience >= expToNextLevel()) {
            magicExperience -= expToNextLevel();
            magicLevel++;
            currentMana = maxMana();
        }
    }

    public int expToNextLevel() {
        return BASE_EXP_TO_LEVEL + (magicLevel - 1) * 40;
    }

    public int magicExperience() {
        return magicExperience;
    }

    public void bindSpellToKey(int keyCode, String spellId) {
        if (!learnedSpells.contains(spellId)) {
            throw new IllegalArgumentException("Spell is not learned: " + spellId);
        }
        keyBinds.put(keyCode, spellId);
    }

    public void unbindKey(int keyCode) {
        keyBinds.remove(keyCode);
    }

    public String spellForKey(int keyCode) {
        return keyBinds.get(keyCode);
    }

    public Map<Integer, String> keyBinds() {
        return Collections.unmodifiableMap(keyBinds);
    }

    public MagicProfileSnapshot snapshot() {
        return new MagicProfileSnapshot(learnedSpells, keyBinds, magicLevel, magicExperience, currentMana);
    }

    public void applySnapshot(MagicProfileSnapshot snapshot) {
        learnedSpells.clear();
        learnedSpells.addAll(snapshot.learnedSpells());
        keyBinds.clear();
        keyBinds.putAll(snapshot.keyBinds());
        magicLevel = Math.max(1, snapshot.magicLevel());
        magicExperience = Math.max(0, snapshot.magicExperience());
        currentMana = Math.max(0, Math.min(snapshot.currentMana(), maxMana()));
    }
}
