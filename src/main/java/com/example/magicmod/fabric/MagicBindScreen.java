package com.example.magicmod.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MagicBindScreen extends Screen {
    private static final LearnedSpellEntry EMPTY = new LearnedSpellEntry("", "Пусто");

    private final MagicUiState state;
    private final List<LearnedSpellEntry> options;

    public MagicBindScreen(MagicUiState state) {
        super(Text.literal("Magic Book"));
        this.state = state;
        this.options = new ArrayList<>();
        this.options.add(EMPTY);
        this.options.addAll(state.learnedSpells());
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 160;
        int top = this.height / 2 - 88;

        for (int i = 0; i < 9; i++) {
            int slot = i + 1;
            int row = i / 3;
            int col = i % 3;
            int x = left + col * 108;
            int y = top + row * 30;

            LearnedSpellEntry initial = findById(state.boundSlots().get(i));
            this.addDrawableChild(CyclingButtonWidget.<LearnedSpellEntry>builder(entry -> Text.literal(entry.displayName()))
                    .values(options)
                    .initially(initial)
                    .build(x, y, 100, 20, Text.literal("Слот " + slot), (button, value) -> sendBindPacket(slot, value.id())));
        }
    }

    private LearnedSpellEntry findById(String id) {
        for (LearnedSpellEntry option : options) {
            if (option.id().equals(id)) {
                return option;
            }
        }
        return EMPTY;
    }

    private void sendBindPacket(int slot, String spellId) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(slot);
        buf.writeString(spellId);
        ClientPlayNetworking.send(MagicModFabric.BIND_SPELL_C2S, buf);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int left = this.width / 2 - 160;
        int top = this.height / 2 - 104;
        context.drawText(this.textRenderer, Text.literal("Бинды заклинаний"), left, top, 0xFFFFFF, false);
    }
}
