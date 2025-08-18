package com.hfstack.royalcrown.network;

import com.hfstack.royalcrown.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RCNetwork {
    private RCNetwork() {}

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ModMain.MODID, "net"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    private static int id = 0;
    private static int nextId() { return id++; }

    public static void init() {
        CHANNEL.registerMessage(nextId(),
                OpenAdvisorScreenS2C.class,
                OpenAdvisorScreenS2C::encode,
                OpenAdvisorScreenS2C::decode,
                OpenAdvisorScreenS2C::handle);

        CHANNEL.registerMessage(nextId(),
                AdvisorActionC2S.class,
                AdvisorActionC2S::encode,
                AdvisorActionC2S::decode,
                AdvisorActionC2S::handle);
    }
}
