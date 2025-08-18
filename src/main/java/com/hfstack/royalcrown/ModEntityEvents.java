package com.hfstack.royalcrown;

import com.hfstack.royalcrown.client.RoyalAdvisorRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    // Atributos do Conselheiro — use os do próprio RoyalAdvisorEntity
    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModRegistry.ROYAL_ADVISOR.get(), RoyalAdvisorEntity.createAttributes().build());
    }

    // Render só no lado cliente
    @Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientOnly {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModRegistry.ROYAL_ADVISOR.get(), RoyalAdvisorRenderer::new);
        }
    }
}
