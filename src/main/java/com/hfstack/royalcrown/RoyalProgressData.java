package com.hfstack.royalcrown;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RoyalProgressData extends SavedData {
    public static final String NAME = "royalcrown_progress";

    // defesas completas por jogador
    private final Map<UUID, Integer> defensesDone = new HashMap<>();

    // controle de unicidade da coroa
    private boolean crownGiven = false;
    private UUID crownOwner = null; // se UNIQUE_PER_WORLD for true, guardamos o dono

    private boolean advisorSpawned = false;

    public static RoyalProgressData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RoyalProgressData::load, RoyalProgressData::new, NAME);
    }

    public RoyalProgressData() {
    }

    public static RoyalProgressData load(CompoundTag nbt) {
        RoyalProgressData data = new RoyalProgressData();
        // já existia
        data.crownGiven = nbt.getBoolean("crownGiven");
        if (nbt.hasUUID("crownOwner")) data.crownOwner = nbt.getUUID("crownOwner");
        // novo:
        data.advisorSpawned = nbt.getBoolean("advisorSpawned");

        ListTag list = nbt.getList("defenses", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag ct = (CompoundTag) t;
            UUID id = ct.getUUID("player");
            int cnt = ct.getInt("count");
            data.defensesDone.put(id, cnt);
        }
        return data;
    }

    public boolean isAdvisorSpawned() {
        return advisorSpawned;
    }

    public void setAdvisorSpawned(boolean v) {
        this.advisorSpawned = v;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        nbt.putBoolean("crownGiven", crownGiven);
        if (crownOwner != null) nbt.putUUID("crownOwner", crownOwner);
        // novo:
        nbt.putBoolean("advisorSpawned", advisorSpawned);

        ListTag list = new ListTag();
        for (var e : defensesDone.entrySet()) {
            CompoundTag ct = new CompoundTag();
            ct.putUUID("player", e.getKey());
            ct.putInt("count", e.getValue());
            list.add(ct);
        }
        nbt.put("defenses", list);
        return nbt;
    }

    public int getDefenses(UUID player) {
        return defensesDone.getOrDefault(player, 0);
    }

    public void addDefense(UUID player, int amt) {
        defensesDone.put(player, getDefenses(player) + amt);
        setDirty();
    }

    public boolean isCrownGiven() {
        return crownGiven;
    }

    public UUID getCrownOwner() {
        return crownOwner;
    }

    public void giveCrownTo(UUID owner) {
        this.crownGiven = true;
        this.crownOwner = owner;
        setDirty();
    }

    public void clearCrown() { // se ALLOW_RECLAIM quiser resetar
        this.crownGiven = false;
        this.crownOwner = null;
        setDirty();
    }
}
