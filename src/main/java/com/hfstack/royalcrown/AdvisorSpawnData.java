package com.hfstack.royalcrown;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

public class AdvisorSpawnData extends SavedData {
    private static final String NAME = "rc_advisor_spawn";

    private boolean pending = false;
    private boolean spawned = false;
    private long due = 0L;
    private BlockPos pos = BlockPos.ZERO;
    private String dim = "";

    // mensagem de pré-chegada (5s)
    private boolean notified = false;
    private long notifyDue = 0L;
    private String notifyPlayer = "";

    public AdvisorSpawnData() {
    }

    public static AdvisorSpawnData load(CompoundTag tag) {
        AdvisorSpawnData d = new AdvisorSpawnData();
        d.pending = tag.getBoolean("Pending");
        d.spawned = tag.getBoolean("Spawned");
        d.due = tag.getLong("Due");
        d.pos = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        d.dim = tag.getString("Dim");
        d.notified = tag.getBoolean("Notified");
        d.notifyDue = tag.getLong("NotifyDue");
        d.notifyPlayer = tag.getString("NotifyPlayer");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Pending", pending);
        tag.putBoolean("Spawned", spawned);
        tag.putLong("Due", due);
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putString("Dim", dim == null ? "" : dim);
        tag.putBoolean("Notified", notified);
        tag.putLong("NotifyDue", notifyDue);
        tag.putString("NotifyPlayer", notifyPlayer == null ? "" : notifyPlayer);
        return tag;
    }

    public static AdvisorSpawnData get(ServerLevel sl) {
        return sl.getDataStorage().computeIfAbsent(AdvisorSpawnData::load, AdvisorSpawnData::new, NAME);
    }

    public void schedule(ServerLevel sl, BlockPos at, long when, ResourceKey<Level> dimension, UUID playerId, int notifyDelayTicks) {
        if (isScheduled() || isSpawned()) return;
        this.pending = true;
        this.pos = at.immutable();
        this.due = when;
        this.dim = dimension.location().toString();
        this.notified = false;
        this.notifyDue = sl.getGameTime() + Math.max(0, notifyDelayTicks);
        this.notifyPlayer = playerId != null ? playerId.toString() : "";
        setDirty();
    }

    public void setSpawned() {
        this.spawned = true;
        this.pending = false;
        this.due = 0L;
        setDirty();
    }

    public void tick(ServerLevel sl) {
        if (!pending) return;
        if (!sl.dimension().location().toString().equals(dim)) return;

        long now = sl.getGameTime();

        // 1) Mensagem de pré-chegada (atrasada)
        if (!notified && now >= notifyDue) {
            Player target = null;
            try {
                if (!notifyPlayer.isEmpty()) target = sl.getPlayerByUUID(UUID.fromString(notifyPlayer));
            } catch (IllegalArgumentException ignored) {
            }
            if (target == null && !sl.players().isEmpty()) {
                target = sl.players().get(0);
            }
            if (target != null) {
                target.sendSystemMessage(Component.translatable("msg.royalcrown.advisor.prearrival"));
            }
            this.notified = true;
            setDirty();
        }

        // 2) Chegou a hora de spawnar
        if (now >= due) {
            RoyalTrials.ensureAdvisorNearTownHall(sl, pos);

            // AVISO DE CHEGADA (para jogadores num raio de 64 blocos)
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            double r2 = 64 * 64;
            for (Player p : sl.players()) {
                if (p.distanceToSqr(x, y, z) <= r2) {
                    p.sendSystemMessage(Component.translatable("msg.royalcrown.advisor.arrived"));
                }
            }

            setSpawned();
        }
    }

    // getters
    public boolean isPending() {
        return pending;
    }

    public boolean isScheduled() {
        return pending;
    }

    public boolean isSpawned() {
        return spawned;
    }

    public long getDue() {
        return due;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getDim() {
        return dim;
    }
}
