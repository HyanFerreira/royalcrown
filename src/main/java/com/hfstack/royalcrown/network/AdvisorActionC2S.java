package com.hfstack.royalcrown.network;

import com.hfstack.royalcrown.RCAdvancements;
import com.hfstack.royalcrown.CoronationCeremony;
import com.hfstack.royalcrown.RoyalTrials;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdvisorActionC2S {

    public enum Action {
        ACCEPT,        // aceitou iniciar as provas (INTRO4)
        CLAIM_CROWN,   // confirmou “Tornar-se Rei”
        FAREWELL_DONE  // terminou despedida → remover Conselheiro
    }

    public final Action action;
    public final int advisorEntityId; // usado em CLAIM_CROWN / FAREWELL_DONE (0 para ACCEPT)

    public AdvisorActionC2S(Action action) {
        this(action, 0);
    }

    public AdvisorActionC2S(Action action, int advisorEntityId) {
        this.action = action;
        this.advisorEntityId = advisorEntityId;
    }

    public static void encode(AdvisorActionC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeVarInt(msg.advisorEntityId);
    }

    public static AdvisorActionC2S decode(FriendlyByteBuf buf) {
        Action a = buf.readEnum(Action.class);
        int id = buf.readVarInt();
        return new AdvisorActionC2S(a, id);
    }

    public static void handle(AdvisorActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sp = ctx.get().getSender();
        if (sp == null) {
            ctx.get().setPacketHandled(true);
            return;
        }
        ServerLevel sl = sp.serverLevel();

        switch (msg.action) {
            case ACCEPT -> {
                sp.getPersistentData().putBoolean("RC_Accepted", true);
                RCAdvancements.grant(sp, RCAdvancements.ACCEPTED);

                // feedback curto (agora via lang)
                sp.sendSystemMessage(Component.translatable("msg.royalcrown.advisor.accepted"));
            }
            case CLAIM_CROWN -> {
                // Recheca requisitos (evita race)
                if (RoyalTrials.canClaimCrown(sl, sp)) {
                    CoronationCeremony.start(sl, sp, msg.advisorEntityId);
                } else {
                    sp.sendSystemMessage(Component.translatable("msg.royalcrown.coronation.not_ready"));
                }
            }
            case FAREWELL_DONE -> {
                var ent = sl.getEntity(msg.advisorEntityId);
                if (ent != null) ent.discard(); // some do mundo
                sp.sendSystemMessage(Component.translatable("msg.royalcrown.advisor.farewell.departed"));
            }
        }

        ctx.get().setPacketHandled(true);
    }
}
