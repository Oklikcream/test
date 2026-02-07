package com.example.magicmod.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MagicBindScreen extends Screen {
    private static final LearnedSpellEntry EMPTY = new LearnedSpellEntry("", "Пусто");

    private final MagicUiState state;
    private String selectedSpell;
    private final List<LearnedSpellEntry> options;

    public MagicBindScreen(MagicUiState state) {
        super(Text.literal("Magic Interface"));
        super(Text.literal("Magic Binds"));
        this.state = state;
        if (!state.learnedSpells().isEmpty()) {
            this.selectedSpell = state.learnedSpells().get(0).id();
        }
        this.options = new ArrayList<>();
        this.options.add(EMPTY);
        this.options.addAll(state.learnedSpells());
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 170;
        int top = this.height / 2 - 90;

        int y = top + 18;
        for (LearnedSpellEntry spell : state.learnedSpells()) {
            int finalY = y;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(spell.displayName() + " (" + spell.id() + ")"), b -> selectedSpell = spell.id())
                    .dimensions(left, finalY, 165, 20)
                    .build());
            y += 22;
        }
        int left = this.width / 2 - 160;
        int top = this.height / 2 - 88;

        for (int slot = 1; slot <= 9; slot++) {
            final int s = slot;
            int row = (slot - 1) / 3;
            int col = (slot - 1) % 3;
            int bx = left + 180 + col * 80;
            int by = top + 20 + row * 44;
        for (int i = 0; i < 9; i++) {
            int slot = i + 1;
            int row = i / 3;
            int col = i % 3;
            int x = left + col * 108;
            int y = top + row * 30;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Бинд " + slot), b -> {
                if (selectedSpell != null && !selectedSpell.isBlank()) {
                    sendBindPacket(s, selectedSpell);
                }
            }).dimensions(bx, by, 74, 20).build());
            LearnedSpellEntry initial = findById(state.boundSlots().get(i));
            this.addDrawableChild(CyclingButtonWidget.builder(entry -> Text.literal(entry.displayName()))
                    .values(options)
                    .initially(initial)
                    .build(x, y, 100, 20, Text.literal("Слот " + slot), (button, value) -> sendBindPacket(slot, value.id())));
        }
    }

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Очистить"), b -> sendBindPacket(s, ""))
                    .dimensions(bx, by + 22, 74, 18).build());
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

        int left = this.width / 2 - 170;
        int top = this.height / 2 - 90;
        context.drawText(this.textRenderer, Text.literal("Магия: ур. " + state.level() + " | Мана: " + state.mana() + "/" + state.maxMana()), left, top, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("Выбрано: " + (selectedSpell == null ? "-" : selectedSpell)), left, top + 95, 0xAAFFAA, false);

        List<String> bound = new ArrayList<>(state.boundSlots());
        for (int i = 0; i < bound.size(); i++) {
            String value = bound.get(i).isBlank() ? "пусто" : bound.get(i);
            context.drawText(this.textRenderer, Text.literal((i + 1) + ": " + value), left, top + 118 + i * 10, 0xDDDDDD, false);
        }
        int left = this.width / 2 - 160;
        int top = this.height / 2 - 104;
        context.drawText(this.textRenderer, Text.literal("Бинды заклинаний"), left, top, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("Мана: " + state.mana() + "/" + state.maxMana() + " | Уровень: " + state.level()), left, top + 12, 0x66CCFF, false);
    }
}
