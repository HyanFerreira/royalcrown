package com.hfstack.royalcrown;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class AdvisorDialogue {

    private AdvisorDialogue() {
    }

    /**
     * Realça o Conselheiro mais próximo com brilho, toca um som e informa a posição.
     */
    public static void pingAdvisor(ServerLevel sl, Player p) {
        final double R = 64.0;
        List<RoyalAdvisorEntity> list = sl.getEntitiesOfClass(
                RoyalAdvisorEntity.class, p.getBoundingBox().inflate(R), e -> true
        );

        if (list.isEmpty()) {
            // Sem conselheiro por perto: tenta achar a Prefeitura mais próxima e orientar o jogador
            BlockPos th = RoyalTrials.findNearestTownHall(sl, p.blockPosition(), 64);
            if (th != null) {
                p.sendSystemMessage(Component.translatable(
                        "msg.royalcrown.advisor.where.coords_th",
                        th.getX(), th.getY(), th.getZ()
                ));
            } else {
                p.sendSystemMessage(Component.translatable(
                        "msg.royalcrown.advisor.where.notfound"
                ));
            }
            return;
        }

        // Achou o conselheiro → destaca e avisa a posição
        RoyalAdvisorEntity adv = list.get(0);
        adv.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false)); // 10s de brilho
        sl.playSound(null, adv.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.2F);

        p.sendSystemMessage(Component.translatable(
                "msg.royalcrown.advisor.where.here",
                adv.blockPosition().getX(),
                adv.blockPosition().getY(),
                adv.blockPosition().getZ()
        ));
    }
}
