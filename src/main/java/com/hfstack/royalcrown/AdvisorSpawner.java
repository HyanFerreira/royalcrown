package com.hfstack.royalcrown;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

// Mantemos a classe registrada, mas sem fazer nada.
// Assim você não tem spawn duplicado e evita crash no login.
@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdvisorSpawner {

    @SubscribeEvent
    public static void onCitizenJoin(EntityJoinLevelEvent event) {
        // Desligado de propósito. O Conselheiro nasce pelo fluxo do Town Hall.
        // Se quiser reativar algum dia, use a Opção B abaixo (com o método corrigido).
    }
}
