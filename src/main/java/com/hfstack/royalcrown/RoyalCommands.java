package com.hfstack.royalcrown;

import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class RoyalCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("royalcrown")
                .then(Commands.literal("help").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    sendHelp(p);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    sendStatus(p);
                    return 1;
                }))
                .then(Commands.literal("debug")
                        .then(Commands.literal("accept").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            p.getPersistentData().putBoolean("RC_Accepted", true);
                            RCAdvancements.grant(p, RCAdvancements.ACCEPTED);
                            p.sendSystemMessage(Component.translatable("cmd.royalcrown.debug.accept"));
                            sendStatus(p);
                            return 1;
                        }))
                        .then(Commands.literal("complete_defenses").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            RoyalProgressData.get(p.serverLevel()).setDefenses(
                                    p.getUUID(),
                                    RCConfig.COMMON.TRIAL_DEFENSES_REQUIRED.get()
                            );
                            p.getPersistentData().putBoolean("RC_Accepted", true);
                            RCAdvancements.grant(p, RCAdvancements.ACCEPTED);
                            p.sendSystemMessage(Component.translatable("cmd.royalcrown.debug.complete_defenses"));
                            sendStatus(p);
                            return 1;
                        }))
                        .then(Commands.literal("reset_player").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            RoyalProgressData.get(p.serverLevel()).resetPlayer(p.getUUID());
                            p.getPersistentData().remove("RC_Accepted");
                            p.getPersistentData().remove("RC_Crowned");
                            p.getPersistentData().remove("RC_ReadyNotified");
                            p.sendSystemMessage(Component.translatable("cmd.royalcrown.debug.reset_player"));
                            sendStatus(p);
                            return 1;
                        }))
                        .then(Commands.literal("clear_crown").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            RoyalProgressData.get(p.serverLevel()).clearCrown();
                            p.sendSystemMessage(Component.translatable("cmd.royalcrown.debug.clear_crown"));
                            sendStatus(p);
                            return 1;
                        }))
                        .then(Commands.literal("give_crown").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            RoyalTrials.giveCrownIfMissing(p);
                            p.getPersistentData().putBoolean("RC_Crowned", true);
                            if (RCConfig.COMMON.UNIQUE_PER_WORLD.get()) {
                                RoyalProgressData.get(p.serverLevel()).giveCrownTo(p.getUUID());
                            }
                            RCAdvancements.grant(p, RCAdvancements.CROWNED);
                            p.sendSystemMessage(Component.translatable("cmd.royalcrown.debug.give_crown"));
                            sendStatus(p);
                            return 1;
                        }))
                        .then(Commands.literal("start_coronation").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            if (!RoyalTrials.canClaimCrown(p.serverLevel(), p)) {
                                p.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.not_ready"));
                                sendStatus(p);
                                return 0;
                            }

                            CoronationCeremony.start(p.serverLevel(), p, 0);
                            return 1;
                        }))
                        .then(Commands.literal("respawn_advisor").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ServerLevel sl = p.serverLevel();
                            BlockPos townHall = RoyalTrials.findNearestTownHall(sl, p.blockPosition(), 96);
                            if (townHall == null) {
                                p.sendSystemMessage(Component.translatable("cmd.royalcrown.debug.respawn_advisor.no_townhall"));
                                return 0;
                            }

                            RoyalTrials.ensureAdvisorNearTownHall(sl, townHall.above());
                            p.sendSystemMessage(Component.translatable(
                                    "cmd.royalcrown.debug.respawn_advisor",
                                    townHall.getX(), townHall.getY(), townHall.getZ()
                            ));
                            return 1;
                        }))
                ));
    }

    private static void sendHelp(ServerPlayer p) {
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.header"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.status"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.advisor_where"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_accept"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_complete_defenses"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_start_coronation"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_reset_player"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_clear_crown"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_give_crown"));
        p.sendSystemMessage(Component.translatable("cmd.royalcrown.help.debug_respawn_advisor"));
    }

    private static void sendStatus(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        int citizens = RoyalTrials.getColonyCitizenCount(sl, p);
        int reqCit = RCConfig.COMMON.TRIAL_REQUIRED_CITIZENS.get();
        RoyalProgressData data = RoyalProgressData.get(sl);
        int defDone = data.getDefenses(p.getUUID());
        int reqDef = RCConfig.COMMON.TRIAL_DEFENSES_REQUIRED.get();
        boolean accepted = p.getPersistentData().getBoolean("RC_Accepted");
        boolean crowned = p.getPersistentData().getBoolean("RC_Crowned");
        boolean ceremony = CoronationCeremony.isRunning(p.getUUID());
        UUID owner = data.getCrownOwner();
        String ownerText = owner == null ? "-" : owner.toString();

        p.sendSystemMessage(Component.translatable(
                "msg.royalcrown.status",
                citizens, reqCit, defDone, reqDef,
                Component.translatable(accepted ? "label.royalcrown.yes" : "label.royalcrown.no"),
                Component.translatable(crowned ? "label.royalcrown.yes" : "label.royalcrown.no"),
                Component.translatable(ceremony ? "label.royalcrown.yes" : "label.royalcrown.no"),
                data.isCrownGiven() ? ownerText : "-"
        ));
    }
}
