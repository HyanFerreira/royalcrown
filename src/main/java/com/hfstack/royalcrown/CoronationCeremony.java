package com.hfstack.royalcrown;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CoronationCeremony {
    private static final ResourceLocation MINECOLONIES_CITIZEN = new ResourceLocation("minecolonies", "citizen");
    private static final int GATHER_TICKS = 220;
    private static final int CELEBRATE_TICKS = 220;
    private static final int TOTAL_TICKS = GATHER_TICKS + CELEBRATE_TICKS;
    private static final double SEARCH_RADIUS = 64.0D;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private CoronationCeremony() {
    }

    public static void start(ServerLevel sl, ServerPlayer player, int advisorEntityId) {
        UUID playerId = player.getUUID();
        if (SESSIONS.containsKey(playerId)) {
            player.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.already_started"));
            return;
        }

        BlockPos anchor = findCeremonyAnchor(sl, player, advisorEntityId);
        List<UUID> participants = findParticipants(sl, anchor);

        SESSIONS.put(playerId, new Session(playerId, sl.dimension().location().toString(), anchor, participants));

        player.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.begin"));
        sl.playSound(null, anchor, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.2F, 0.8F);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, anchor.getX() + 0.5D, anchor.getY() + 1.5D, anchor.getZ() + 0.5D,
                24, 2.5D, 1.0D, 2.5D, 0.02D);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.level instanceof ServerLevel sl)) return;
        if (SESSIONS.isEmpty()) return;

        String dim = sl.dimension().location().toString();
        List<UUID> done = new ArrayList<>();

        for (Session session : SESSIONS.values()) {
            if (!session.dimension.equals(dim)) continue;

            Player foundPlayer = sl.getPlayerByUUID(session.playerId);
            if (!(foundPlayer instanceof ServerPlayer player) || !player.isAlive()) {
                done.add(session.playerId);
                continue;
            }

            session.age++;
            tickSession(sl, player, session);

            if (session.age >= TOTAL_TICKS) {
                finish(sl, player, session);
                done.add(session.playerId);
            }
        }

        for (UUID id : done) {
            SESSIONS.remove(id);
        }
    }

    private static void tickSession(ServerLevel sl, ServerPlayer player, Session session) {
        if (session.age % 20 == 1) {
            guideParticipants(sl, player, session);
        }

        if (session.age == GATHER_TICKS) {
            sl.playSound(null, session.anchor, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 0.9F);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.2D, player.getZ(),
                    36, 0.7D, 0.8D, 0.7D, 0.08D);
            player.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.witness"));
        }

        if (session.age > GATHER_TICKS && session.age % 10 == 0) {
            cheerParticipants(sl, player, session);
        }

        if (session.age > GATHER_TICKS && session.age % 60 == 0) {
            sl.playSound(null, session.anchor, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.9F, 1.2F);
        }
    }

    private static void finish(ServerLevel sl, ServerPlayer player, Session session) {
        boolean crowned = RoyalTrials.completeCoronation(sl, player);

        if (crowned) {
            cheerParticipants(sl, player, session);
            sl.sendParticles(ParticleTypes.FIREWORK,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    40, 1.0D, 1.0D, 1.0D, 0.08D);
            player.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.complete"));
        } else {
            player.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.not_ready"));
        }
    }

    public static boolean isRunning(UUID playerId) {
        return SESSIONS.containsKey(playerId);
    }

    private static BlockPos findCeremonyAnchor(ServerLevel sl, ServerPlayer player, int advisorEntityId) {
        BlockPos townHall = RoyalTrials.findNearestTownHall(sl, player.blockPosition(), 96);
        if (townHall != null) return townHall.above();

        Entity advisor = sl.getEntity(advisorEntityId);
        if (advisor instanceof RoyalAdvisorEntity royalAdvisor && royalAdvisor.getHome() != null) {
            return royalAdvisor.getHome().above();
        }
        if (advisor != null) return advisor.blockPosition();
        return player.blockPosition();
    }

    private static List<UUID> findParticipants(ServerLevel sl, BlockPos anchor) {
        AABB area = new AABB(anchor).inflate(SEARCH_RADIUS);
        List<Mob> citizens = sl.getEntitiesOfClass(Mob.class, area, CoronationCeremony::isMinecoloniesCitizen);
        List<UUID> ids = new ArrayList<>();
        for (Mob mob : citizens) {
            ids.add(mob.getUUID());
        }
        return ids;
    }

    private static void guideParticipants(ServerLevel sl, ServerPlayer player, Session session) {
        int index = 0;
        for (UUID id : session.participants) {
            Entity entity = sl.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;

            Vec3 spot = ceremonySpot(session.anchor, index++, isLikelyGuard(mob));
            mob.getNavigation().moveTo(spot.x, spot.y, spot.z, isLikelyGuard(mob) ? 0.85D : 0.75D);
            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
    }

    private static void cheerParticipants(ServerLevel sl, Player player, Session session) {
        for (UUID id : session.participants) {
            Entity entity = sl.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;

            mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (mob.onGround() && sl.random.nextFloat() < 0.35F) {
                mob.getJumpControl().jump();
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        mob.getX(), mob.getY() + mob.getBbHeight() + 0.2D, mob.getZ(),
                        2, 0.2D, 0.2D, 0.2D, 0.01D);
            }
        }
    }

    private static Vec3 ceremonySpot(BlockPos anchor, int index, boolean guard) {
        double radius = guard ? 4.0D : 6.0D;
        double angle = (index * 57.0D) * Math.PI / 180.0D;
        double x = anchor.getX() + 0.5D + Math.cos(angle) * radius;
        double z = anchor.getZ() + 0.5D + Math.sin(angle) * radius;
        return new Vec3(x, anchor.getY(), z);
    }

    private static boolean isMinecoloniesCitizen(Mob mob) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return key != null && key.equals(MINECOLONIES_CITIZEN);
    }

    private static boolean isLikelyGuard(Mob mob) {
        Item main = mob.getMainHandItem().getItem();
        return main instanceof SwordItem
                || main instanceof AxeItem
                || main instanceof BowItem
                || main instanceof CrossbowItem;
    }

    private static final class Session {
        private final UUID playerId;
        private final String dimension;
        private final BlockPos anchor;
        private final List<UUID> participants;
        private int age;

        private Session(UUID playerId, String dimension, BlockPos anchor, List<UUID> participants) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.anchor = anchor.immutable();
            this.participants = participants;
        }
    }
}
