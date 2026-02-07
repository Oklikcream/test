package com.example.magicmod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ArcaneWorkbench {
    public static final int GRID_SIZE = 9;
    private final Map<String, String> spellRecipes = new HashMap<>();
    private final Map<String, String> explosionRecipes = new HashMap<>();

    public void registerSpellRecipe(String pattern81, String spellId) {
        validatePattern(pattern81);
        spellRecipes.put(pattern81, spellId);
    }

    public void registerExplosionRecipe(String pattern81, String reason) {
        validatePattern(pattern81);
        explosionRecipes.put(pattern81, reason);
    }

    public ArcaneCraftingResult craft(String pattern81) {
        validatePattern(pattern81);
        if (spellRecipes.containsKey(pattern81)) {
            return ArcaneCraftingResult.spell(spellRecipes.get(pattern81));
        }
        if (explosionRecipes.containsKey(pattern81)) {
            return ArcaneCraftingResult.explosion();
        }
        return ArcaneCraftingResult.nothing();
    }

    private static void validatePattern(String pattern81) {
        Objects.requireNonNull(pattern81, "pattern81");
        if (pattern81.length() != GRID_SIZE * GRID_SIZE) {
            throw new IllegalArgumentException("Pattern must have exactly 81 symbols for a 9x9 grid");
        }
    }
}
