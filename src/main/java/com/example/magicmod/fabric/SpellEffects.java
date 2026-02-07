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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SpellEffects {
    private SpellEffects() {
    }

    public static boolean cast(ServerPlayerEntity caster, String spellId) {
        return switch (spellId) {
            case "fireball" -> castFireball(caster);
            case "blink" -> castBlink(caster);
            case "ice_spike" -> castIceSpike(caster);
            default -> false;
        };
    }

    private static boolean castFireball(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        HitResult hit = caster.raycast(24.0, 0.0F, false);
        Vec3d target = hit.getType() == HitResult.Type.MISS
                ? caster.getPos().add(caster.getRotationVec(1.0F).multiply(16.0))
                : hit.getPos();

        world.createExplosion(caster, target.x, target.y, target.z, 2.7F, World.ExplosionSourceType.MOB);
        world.spawnParticles(ParticleTypes.FLAME, target.x, target.y + 0.2, target.z, 20, 0.8, 0.3, 0.8, 0.02);
        world.playSound(null, target.x, target.y, target.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.9F, 0.8F);
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

        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, caster.getBoundingBox().expand(4.0), e -> e != caster)) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 1));
            affected++;
        }

        world.spawnParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 1.0, pos.z, 70, 2.5, 1.0, 2.5, 0.02);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.8F, 0.7F);
        return affected > 0;
    }
}
