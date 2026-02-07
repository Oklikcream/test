package com.example.magicmod.fabric.mixin;

import com.example.magicmod.PlayerMagicProfile;
import com.example.magicmod.fabric.MagicPlayerDataHolder;
import com.example.magicmod.fabric.MagicProfileNbt;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements MagicPlayerDataHolder {
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
}
