package com.example.magicmod.fabric;

import com.example.magicmod.PlayerMagicProfile;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MagicPlayerData {
    private MagicPlayerData() {
    }

    public static PlayerMagicProfile get(ServerPlayerEntity player) {
        return ((MagicPlayerDataHolder) player).magicmod$getProfile();
    }
}
