// java/com/hfstack/royalcrown/client/ClientHudHider.java
package com.hfstack.royalcrown.client;

import com.hfstack.royalcrown.ModMain;
import com.hfstack.royalcrown.client.gui.AdvisorScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientHudHider {

    private ClientHudHider() {
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!isCrosshair(e)) return;

        // Esconde a mira somente quando o AdvisorScreen está aberto
        if (Minecraft.getInstance().screen instanceof AdvisorScreen) {
            e.setCanceled(true);
        }
    }

    // Compat: algumas versões expõem .id(), outras .type()
    private static boolean isCrosshair(RenderGuiOverlayEvent.Pre e) {
        try {
            return e.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id());
        } catch (Throwable ignore) {
            try {
                return e.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type();
            } catch (Throwable ignore2) {
                return false;
            }
        }
    }
}
