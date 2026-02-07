package com.example.magicmod.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MagicBindScreen extends Screen {
    private final MagicUiState state;
    private String selectedSpell;

    public MagicBindScreen(MagicUiState state) {
        super(Text.literal("Magic Interface"));
        this.state = state;
        if (!state.learnedSpells().isEmpty()) {
            this.selectedSpell = state.learnedSpells().get(0);
        }
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 170;
        int top = this.height / 2 - 90;

        int y = top + 18;
        for (String spellId : state.learnedSpells()) {
            int finalY = y;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(spellId), b -> selectedSpell = spellId)
                    .dimensions(left, finalY, 100, 20)
                    .build());
            y += 22;
        }

        for (int slot = 1; slot <= 9; slot++) {
            final int s = slot;
            int row = (slot - 1) / 3;
            int col = (slot - 1) % 3;
            int bx = left + 120 + col * 80;
            int by = top + 20 + row * 44;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Бинд " + slot), b -> {
                if (selectedSpell != null && !selectedSpell.isBlank()) {
                    sendBindPacket(s, selectedSpell);
                }
            }).dimensions(bx, by, 74, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Каст"), b -> sendCastPacket(s))
                    .dimensions(bx, by + 22, 36, 18).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> sendBindPacket(s, ""))
                    .dimensions(bx + 38, by + 22, 36, 18).build());
        }
    }

    private void sendBindPacket(int slot, String spellId) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(slot);
        buf.writeString(spellId);
        ClientPlayNetworking.send(MagicModFabric.BIND_SPELL_C2S, buf);
    }

    private void sendCastPacket(int slot) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(slot);
        ClientPlayNetworking.send(MagicModFabric.CAST_SPELL_C2S, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int left = this.width / 2 - 170;
        int top = this.height / 2 - 90;
        context.drawText(this.textRenderer, Text.literal("Магия: ур. " + state.level() + " | Мана: " + state.mana() + "/" + state.maxMana()), left, top, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("Выбрано: " + (selectedSpell == null ? "-" : selectedSpell)), left, top + 95, 0xAAFFAA, false);

        List<String> bound = new ArrayList<>(state.boundSlots());
        for (int i = 0; i < bound.size(); i++) {
            String value = bound.get(i).isBlank() ? "пусто" : bound.get(i);
            context.drawText(this.textRenderer, Text.literal((i + 1) + ": " + value), left, top + 112 + i * 10, 0xDDDDDD, false);
        }
    }
}
