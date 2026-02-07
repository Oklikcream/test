package com.example.magicmod.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public class MagicModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MagicModFabric.OPEN_MAGIC_UI_S2C, (client, handler, buf, responseSender) -> {
            int level = buf.readInt();
            int mana = buf.readInt();
            int maxMana = buf.readInt();

            int learnedSize = buf.readInt();
            List<String> learned = new ArrayList<>();
            for (int i = 0; i < learnedSize; i++) {
                learned.add(buf.readString());
            }

            List<String> bound = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                bound.add(buf.readString());
            }

            MagicUiState state = new MagicUiState(level, mana, maxMana, learned, bound);
            client.execute(() -> MinecraftClient.getInstance().setScreen(new MagicBindScreen(state)));
        });
    }
}
