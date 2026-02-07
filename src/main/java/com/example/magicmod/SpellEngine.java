package com.example.magicmod;

public final class SpellEngine {
    private static final int EXP_FOR_NEW_SPELL_CRAFT = 35;
    private static final int EXP_FOR_CAST = 10;

    private final SpellRegistry registry;

    public SpellEngine(SpellRegistry registry) {
        this.registry = registry;
    }

    public CastResult castBoundSpellDetailed(PlayerMagicProfile profile, int keyCode) {
        String spellId = profile.spellForKey(keyCode);
        if (spellId == null) {
            return CastResult.NOT_BOUND;
        }
        if (!profile.hasSpell(spellId)) {
            return CastResult.NOT_LEARNED;
        }
        Spell spell = registry.get(spellId);
        if (spell == null) {
            return CastResult.UNKNOWN_SPELL;
        }
        if (profile.magicLevel() < spell.levelRequirement()) {
            return CastResult.LEVEL_TOO_LOW;
        }
        if (!profile.spendMana(spell.manaCost())) {
            return CastResult.NOT_ENOUGH_MANA;
        }
        profile.grantExperience(EXP_FOR_CAST);
        return CastResult.SUCCESS;
    }

    public boolean castBoundSpell(PlayerMagicProfile profile, int keyCode) {
        return castBoundSpellDetailed(profile, keyCode) == CastResult.SUCCESS;
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

    public enum CastResult {
        SUCCESS,
        NOT_BOUND,
        NOT_LEARNED,
        UNKNOWN_SPELL,
        LEVEL_TOO_LOW,
        NOT_ENOUGH_MANA
    }
}
