package com.hfstack.royalcrown.network;

import com.hfstack.royalcrown.client.gui.AdvisorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenAdvisorScreenS2C {

    public enum Mode {
        INTRO1, INTRO2, INTRO3, INTRO4, STATUS,
        CLAIM1, CLAIM2,
        FAREWELL1, FAREWELL2, FAREWELL3
    }

    public final Mode mode;
    public final int reqCit, reqDef, haveCit, defDone;
    /**
     * Id da entidade do Conselheiro (necessário em CLAIM/FAREWELL)
     */
    public final int advisorEntityId;

    public OpenAdvisorScreenS2C(Mode mode, int reqCit, int reqDef, int haveCit, int defDone) {
        this(mode, reqCit, reqDef, haveCit, defDone, 0);
    }

    public OpenAdvisorScreenS2C(Mode mode, int reqCit, int reqDef, int haveCit, int defDone, int advisorEntityId) {
        this.mode = mode;
        this.reqCit = reqCit;
        this.reqDef = reqDef;
        this.haveCit = haveCit;
        this.defDone = defDone;
        this.advisorEntityId = advisorEntityId;
    }

    public static void encode(OpenAdvisorScreenS2C msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.mode);
        buf.writeVarInt(msg.reqCit);
        buf.writeVarInt(msg.reqDef);
        buf.writeVarInt(msg.haveCit);
        buf.writeVarInt(msg.defDone);
        buf.writeVarInt(msg.advisorEntityId);
    }

    public static OpenAdvisorScreenS2C decode(FriendlyByteBuf buf) {
        Mode m = buf.readEnum(Mode.class);
        int rc = buf.readVarInt();
        int rd = buf.readVarInt();
        int hc = buf.readVarInt();
        int dd = buf.readVarInt();
        int id = buf.readVarInt();
        return new OpenAdvisorScreenS2C(m, rc, rd, hc, dd, id);
    }

    public static void handle(OpenAdvisorScreenS2C msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AdvisorScreen(msg.mode, msg.reqCit, msg.reqDef, msg.haveCit, msg.defDone, msg.advisorEntityId));
        });
        ctx.get().setPacketHandled(true);
    }
}
