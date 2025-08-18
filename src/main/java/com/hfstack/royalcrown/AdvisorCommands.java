package com.hfstack.royalcrown;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AdvisorCommands {

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();

        d.register(Commands.literal("royalcrown").then(Commands.literal("advisor")
                // Mantido apenas o /royalcrown advisor where
                .then(Commands.literal("where").executes(ctx -> {
                    ServerPlayer sp = ctx.getSource().getPlayerOrException();
                    ServerLevel sl = sp.serverLevel();

                    double R = 64.0;
                    List<RoyalAdvisorEntity> list = sl.getEntitiesOfClass(RoyalAdvisorEntity.class, sp.getBoundingBox().inflate(R), ent -> true);

                    if (list.isEmpty()) {
                        BlockPos th = RoyalTrials.findNearestTownHall(sl, sp.blockPosition(), 64);
                        if (th != null) {
                            sp.sendSystemMessage(Component.translatable("cmd.royalcrown.advisor.where.th", th.getX(), th.getY(), th.getZ()));
                        } else {
                            sp.sendSystemMessage(Component.translatable("cmd.royalcrown.advisor.where.not_found"));
                        }
                    } else {
                        RoyalAdvisorEntity adv = list.get(0);
                        // brilha por 10s para facilitar achar
                        adv.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
                        // som agradável
                        sl.playSound(null, adv.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.2F);

                        BlockPos bp = adv.blockPosition();
                        sp.sendSystemMessage(Component.translatable("cmd.royalcrown.advisor.where.here", bp.getX(), bp.getY(), bp.getZ()));
                    }
                    return 1;
                }))));
    }
}
