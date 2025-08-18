package com.hfstack.royalcrown;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class RoyalAdvisorData extends SavedData {
    public static final String NAME = "royalcrown_advisor";

    private boolean spawned = false;

    public static RoyalAdvisorData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(RoyalAdvisorData::load, RoyalAdvisorData::new, NAME);
    }

    public RoyalAdvisorData() {
    }

    public static RoyalAdvisorData load(CompoundTag nbt) {
        RoyalAdvisorData d = new RoyalAdvisorData();
        d.spawned = nbt.getBoolean("spawned");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("spawned", spawned);
        return tag;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public void setSpawned() {
        this.spawned = true;
        setDirty();
    }
}
