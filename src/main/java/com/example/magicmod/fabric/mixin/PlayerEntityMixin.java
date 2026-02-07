package com.example.magicmod.fabric.mixin;

import com.example.magicmod.PlayerMagicProfile;
import com.example.magicmod.fabric.MagicPlayerDataHolder;
import com.example.magicmod.fabric.MagicModFabric;
import com.example.magicmod.fabric.MagicProfileNbt;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements MagicPlayerDataHolder {
    @Unique
    private static final int MANA_REGEN_EVERY_TICKS = 20;

    @Unique
    private static final int MANA_REGEN_AMOUNT = 2;

    @Unique
    private final PlayerMagicProfile magicmod$profile = new PlayerMagicProfile();

    @Override
    public PlayerMagicProfile magicmod$getProfile() {
        return magicmod$profile;
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void magicmod$writeMagicData(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("magicmod:profile", MagicProfileNbt.write(magicmod$profile));
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void magicmod$readMagicData(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("magicmod:profile")) {
            MagicProfileNbt.readInto(magicmod$profile, nbt.getCompound("magicmod:profile"));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void magicmod$regenMana(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!(player instanceof ServerPlayerEntity) || player.getWorld().getTime() % MANA_REGEN_EVERY_TICKS != 0) {
            return;
        }
        magicmod$profile.regenerateMana(MANA_REGEN_AMOUNT);
        MagicModFabric.sendHudSyncPacket((ServerPlayerEntity) player);
    }
}
