package com.example.magicmod.fabric;

import com.example.magicmod.MagicProfileSnapshot;
import com.example.magicmod.PlayerMagicProfile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MagicProfileNbt {
    private MagicProfileNbt() {
    }

    public static NbtCompound write(PlayerMagicProfile profile) {
        MagicProfileSnapshot snapshot = profile.snapshot();
        NbtCompound root = new NbtCompound();

        NbtList spells = new NbtList();
        for (String spellId : snapshot.learnedSpells()) {
            spells.add(NbtString.of(spellId));
        }
        root.put("learnedSpells", spells);

        NbtCompound binds = new NbtCompound();
        for (Map.Entry<Integer, String> entry : snapshot.keyBinds().entrySet()) {
            binds.putString(Integer.toString(entry.getKey()), entry.getValue());
        }
        root.put("keyBinds", binds);

        root.putInt("magicLevel", snapshot.magicLevel());
        root.putInt("magicExperience", snapshot.magicExperience());
        root.putInt("currentMana", snapshot.currentMana());

        return root;
    }

    public static void readInto(PlayerMagicProfile profile, NbtCompound root) {
        Set<String> learned = new HashSet<>();
        NbtList spells = root.getList("learnedSpells", NbtElement.STRING_TYPE);
        for (NbtElement spellElement : spells) {
            learned.add(spellElement.asString());
        }

        Map<Integer, String> binds = new HashMap<>();
        NbtCompound keyBinds = root.getCompound("keyBinds");
        for (String key : keyBinds.getKeys()) {
            binds.put(Integer.parseInt(key), keyBinds.getString(key));
        }

        profile.applySnapshot(new MagicProfileSnapshot(
                learned,
                binds,
                root.getInt("magicLevel"),
                root.getInt("magicExperience"),
                root.getInt("currentMana")
        ));
    }
}
