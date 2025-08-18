package com.hfstack.royalcrown;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RoyalCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("royalcrown").then(Commands.literal("status").executes(ctx -> {
            var p = ctx.getSource().getPlayerOrException();
            var sl = p.serverLevel();

            int citizens = RoyalTrials.countCitizensAround(sl, p, RCConfig.COMMON.TRIAL_NEAR_RADIUS.get());
            int reqCit = RCConfig.COMMON.TRIAL_REQUIRED_CITIZENS.get();
            int defDone = RoyalProgressData.get(sl).getDefenses(p.getUUID());
            int reqDef = RCConfig.COMMON.TRIAL_DEFENSES_REQUIRED.get();

            p.sendSystemMessage(Component.translatable("msg.royalcrown.status", citizens, reqCit, defDone, reqDef));
            return 1;
        })));
    }
}
