package com.example.magicmod;

public final class SpellEngine {
    private static final int EXP_FOR_NEW_SPELL_CRAFT = 35;
    private static final int EXP_FOR_CAST = 10;

    private final SpellRegistry registry;

    public SpellEngine(SpellRegistry registry) {
        this.registry = registry;
    }

    public boolean castBoundSpell(PlayerMagicProfile profile, int keyCode) {
        String spellId = profile.spellForKey(keyCode);
        if (spellId == null || !profile.hasSpell(spellId)) {
            return false;
        }
        Spell spell = registry.get(spellId);
        if (spell == null || profile.magicLevel() < spell.levelRequirement()) {
            return false;
        }
        if (!profile.spendMana(spell.manaCost())) {
            return false;
        }
        profile.grantExperience(EXP_FOR_CAST);
        return true;
    }

    public boolean applyCraftingResult(PlayerMagicProfile profile, ArcaneCraftingResult result) {
        if (result.type() == ArcaneCraftingResult.ResultType.SPELL_SCROLL) {
            boolean isNew = profile.learnSpell(result.spellId());
            if (isNew) {
                profile.grantExperience(EXP_FOR_NEW_SPELL_CRAFT);
            }
            return true;
        }
        return result.type() == ArcaneCraftingResult.ResultType.NOTHING;
    }
}
