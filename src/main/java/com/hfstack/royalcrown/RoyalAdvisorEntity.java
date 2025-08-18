package com.hfstack.royalcrown;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;


import java.util.EnumSet;

public class RoyalAdvisorEntity extends PathfinderMob {

    private BlockPos home;          // posição da Prefeitura
    private int homeRadius = 14; // “orbitar” ~14 blocos

    protected RoyalAdvisorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(); // não despawnar
        this.setCustomName(Component.literal("§bConselheiro Real"));
        this.setCustomNameVisible(true);
        this.getPersistentData().putBoolean("RoyalAdvisor", true);
    }

    // ======== Atributos/Goals básicos ========
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // andar aleatório, mas vamos clamp no tick (StayNearHomeGoal)
        this.goalSelector.addGoal(2, new StayNearHomeGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    // Invulnerável
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    // Interação: delega pro nosso handler já existente (não spamma ao segurar)
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) return InteractionResult.SUCCESS;
        // Reaproveita a lógica já feita no RoyalTrials.onAdvisorInteract
        // (ele verifica a tag RoyalAdvisor e faz o resto).
        player.swing(hand, true);
        return InteractionResult.CONSUME;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }


    // RoyalAdvisorEntity.java (exemplo)
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)     // tanque
                .add(Attributes.MOVEMENT_SPEED, 0.30) // anda normal
                .add(Attributes.FOLLOW_RANGE, 32.0);  // enxerga razoável
    }


    // ======== Casa / raio ========
    public void setHome(BlockPos pos, int radius) {
        this.home = pos.immutable();
        this.homeRadius = Math.max(6, radius);
    }

    public BlockPos getHome() {
        return home;
    }

    public int getHomeRadius() {
        return homeRadius;
    }

    // Mantém por perto da casa; se afastar demais, anda de volta
    static class StayNearHomeGoal extends Goal {
        private final RoyalAdvisorEntity mob;

        StayNearHomeGoal(RoyalAdvisorEntity m) {
            this.mob = m;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos home = mob.getHome();
            if (home == null) return false;
            return mob.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                    > (mob.getHomeRadius() * 0.6) * (mob.getHomeRadius() * 0.6);
        }

        @Override
        public void tick() {
            BlockPos home = mob.getHome();
            if (home == null) return;
            Vec3 target = new Vec3(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            mob.getNavigation().moveTo(target.x, target.y, target.z, 0.8D);
        }
    }

    // ======== salvar/carregar NBT ========
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
            tag.putInt("HomeR", homeRadius);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) {
            this.home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            this.homeRadius = Math.max(6, tag.getInt("HomeR"));
        }
        this.getPersistentData().putBoolean("RoyalAdvisor", true);
    }
}
