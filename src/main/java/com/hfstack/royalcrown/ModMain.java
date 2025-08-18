package com.hfstack.royalcrown;

import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ModMain.MODID)
public class ModMain {

    public static final String MODID = "royalcrown";
    public static final Logger LOGGER = LogManager.getLogger();

    public ModMain() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.BLOCKS.register(eventBus);
        ModRegistry.ITEMS.register(eventBus);
        ModRegistry.TILE_ENTITIES.register(eventBus);
        ModRegistry.ENTITIES.register(eventBus);
        // dentro do construtor:
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RCConfig.COMMON_SPEC, "royalcrown-common.toml");

        eventBus.addListener(this::setup);
        eventBus.addListener(this::setupClient);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            com.hfstack.royalcrown.network.RCNetwork.init(); // << ADICIONE
            CrownEvents.register();
            RoyalTrials.register();
            MinecraftForge.EVENT_BUS.register(new RoyalCommands());
        });
    }

    private void setupClient(final FMLClientSetupEvent event) {
        // client-only
    }
}
