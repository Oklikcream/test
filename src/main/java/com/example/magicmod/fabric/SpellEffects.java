package com.example.magicmod.fabric;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

public final class SpellEffects {
    private static final double PROJECTILE_STEP = 0.7;

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

    public static void triggerMiscastExplosion(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        Vec3d center = caster.getPos();
        world.createExplosion(caster, center.x, center.y, center.z, 2.0F, World.ExplosionSourceType.MOB);
        applyAreaMagicDamage(world, caster, center, 4.0, 8.0F, true);
    }

    private static boolean castFireball(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        Vec3d start = caster.getEyePos();
        Vec3d direction = caster.getRotationVec(1.0F).normalize();
        ProjectilePath path = traceProjectile(world, caster, start, direction, 24.0, ParticleTypes.SMALL_FLAME);

        world.playSound(null, start.x, start.y, start.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.9F, 1.0F);
        world.createExplosion(caster, path.impact().x, path.impact().y, path.impact().z, 2.4F, World.ExplosionSourceType.MOB);
        applyAreaMagicDamage(world, caster, path.impact(), 4.5, 9.0F, true);
        world.spawnParticles(ParticleTypes.FLAME, path.impact().x, path.impact().y + 0.2, path.impact().z, 24, 0.8, 0.3, 0.8, 0.03);
        return true;
    }

    private static boolean castBlink(ServerPlayerEntity caster) {
        ServerWorld world = caster.getServerWorld();
        Vec3d from = caster.getPos();
        Vec3d start = caster.getEyePos();
        Vec3d direction = caster.getRotationVec(1.0F).normalize();
        ProjectilePath path = traceProjectile(world, caster, start, direction, 12.0, ParticleTypes.PORTAL);

        Vec3d destination = path.hitAnything()
                ? path.impact().subtract(direction.multiply(1.0))
                : path.impact();

        caster.requestTeleport(destination.x, destination.y, destination.z);
        world.spawnParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 35, 0.3, 0.8, 0.3, 0.02);
        world.spawnParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1.0, destination.z, 35, 0.3, 0.8, 0.3, 0.02);
        world.playSound(null, destination.x, destination.y, destination.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.7F, 1.2F);
        return true;
    }

    private static ProjectilePath traceProjectile(ServerWorld world, ServerPlayerEntity caster, Vec3d start, Vec3d direction, double maxDistance, ParticleEffect trailParticle) {
        Vec3d previous = start;
        Vec3d impact = start.add(direction.multiply(maxDistance));
        boolean hitAnything = false;

        for (double traveled = PROJECTILE_STEP; traveled <= maxDistance; traveled += PROJECTILE_STEP) {
            Vec3d current = start.add(direction.multiply(traveled));
            BlockHitResult blockHit = world.raycast(new RaycastContext(previous, current, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, caster));
            EntityHitResult entityHit = raycastEntity(world, caster, previous, current);

            world.spawnParticles(trailParticle, current.x, current.y, current.z, 2, 0.04, 0.04, 0.04, 0.0);

            if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS || entityHit.getPos().squaredDistanceTo(previous) <= blockHit.getPos().squaredDistanceTo(previous))) {
                impact = entityHit.getPos();
                hitAnything = true;
                break;
            }
            if (blockHit.getType() != HitResult.Type.MISS) {
                impact = blockHit.getPos();
                hitAnything = true;
                break;
            }

            impact = current;
            previous = current;
        }
        return new ProjectilePath(impact, hitAnything);
    }

    private static EntityHitResult raycastEntity(ServerWorld world, ServerPlayerEntity caster, Vec3d start, Vec3d end) {
        Box scanBox = new Box(start, end).expand(0.4);
        List<Entity> entities = world.getOtherEntities(caster, scanBox,
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity.isAttackable());

        return entities.stream()
                .map(entity -> raycastEntityBox(entity, start, end))
                .filter(result -> result != null)
                .min(Comparator.comparingDouble(result -> result.getPos().squaredDistanceTo(start)))
                .orElse(null);
    }

    private static EntityHitResult raycastEntityBox(Entity entity, Vec3d start, Vec3d end) {
        return entity.getBoundingBox().expand(0.2).raycast(start, end)
                .map(hitPos -> new EntityHitResult(entity, hitPos))
                .orElse(null);
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

    private static void applyAreaMagicDamage(ServerWorld world, ServerPlayerEntity caster, Vec3d center, double radius, float damage, boolean includeCaster) {
        double radiusSquared = radius * radius;
        Box area = Box.of(center, radius * 2.0, radius * 2.0, radius * 2.0);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, area,
                entity -> (includeCaster || entity != caster) && entity.squaredDistanceTo(center) <= radiusSquared);
        for (LivingEntity entity : targets) {
            entity.damage(world.getDamageSources().magic(), damage);
        }
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

    private record ProjectilePath(Vec3d impact, boolean hitAnything) {
    }
}
