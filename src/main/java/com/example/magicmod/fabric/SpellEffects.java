package com.example.magicmod.fabric;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public final class SpellEffects {
    private SpellEffects() {
    }

    public static boolean cast(ServerPlayerEntity caster, String spellId) {
        return switch (spellId) {
            case "fireball" -> castFireball(caster);
            case "blink" -> castBlink(caster);
            case "ice_spike" -> castIceSpike(caster);
            case "arcane_pulse" -> castArcanePulse(caster);
            case "healing_wave" -> castHealingWave(caster);
            default -> false;
        };
    }

    private static boolean castFireball(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        HitResult hit = caster.raycast(24.0, 0.0F, false);
        Vec3d target = hit.getType() == HitResult.Type.MISS
                ? caster.getPos().add(caster.getRotationVec(1.0F).multiply(16.0))
                : hit.getPos();

        world.createExplosion(caster, target.x, target.y, target.z, 2.4F, World.ExplosionSourceType.MOB);
        world.spawnParticles(ParticleTypes.FLAME, target.x, target.y + 0.2, target.z, 24, 0.8, 0.3, 0.8, 0.03);
        world.playSound(null, target.x, target.y, target.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.9F, 0.85F);
        return true;
    }

    private static boolean castBlink(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        Vec3d direction = caster.getRotationVec(1.0F).normalize();
        Vec3d from = caster.getPos();
        Vec3d to = from.add(direction.multiply(8.0));

        caster.requestTeleport(to.x, to.y, to.z);
        world.spawnParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 35, 0.3, 0.8, 0.3, 0.02);
        world.spawnParticles(ParticleTypes.PORTAL, to.x, to.y + 1.0, to.z, 35, 0.3, 0.8, 0.3, 0.02);
        world.playSound(null, to.x, to.y, to.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.7F, 1.2F);
        return true;
    }

    private static boolean castIceSpike(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        Vec3d pos = caster.getPos();
        int affected = 0;

        List<LivingEntity> targets = nearbyTargets(world, caster, caster.getBoundingBox().expand(4.0));
        for (LivingEntity entity : targets) {
            entity.damage(world.getDamageSources().magic(), 5.0F);
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
            affected++;
        }

        world.spawnParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z, 70, 2.5, 1.0, 2.5, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, 0.7F);
        return affected > 0;
    }

    private static boolean castArcanePulse(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        Vec3d center = caster.getPos().add(0, 1.0, 0);
        int affected = 0;
        List<LivingEntity> targets = nearbyTargets(world, caster, Box.of(center, 6.0, 3.0, 6.0));
        for (LivingEntity entity : targets) {
            Vec3d push = entity.getPos().subtract(caster.getPos()).normalize().multiply(1.2);
            entity.damage(world.getDamageSources().magic(), 4.0F);
            entity.addVelocity(push.x, 0.4, push.z);
            entity.velocityModified = true;
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 60, 0));
            affected++;
        }
        world.spawnParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 60, 2.0, 1.0, 2.0, 0.05);
        world.playSound(null, center.x, center.y, center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8F, 1.0F);
        return affected > 0;
    }

    private static List<LivingEntity> nearbyTargets(ServerWorld world, ServerPlayerEntity caster, Box area) {
        return world.getEntitiesByClass(LivingEntity.class, area, entity -> entity != caster);
    }

    private static boolean castHealingWave(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        float healed = Math.min(caster.getMaxHealth(), caster.getHealth() + 6.0F);
        caster.setHealth(healed);
        caster.removeStatusEffect(StatusEffects.POISON);
        caster.removeStatusEffect(StatusEffects.WITHER);
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0));

        Vec3d pos = caster.getPos();
        world.spawnParticles(ParticleTypes.HEART, pos.x, pos.y + 1.2, pos.z, 12, 0.6, 0.6, 0.6, 0.01);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.9F, 1.1F);
        return true;
    }
}
