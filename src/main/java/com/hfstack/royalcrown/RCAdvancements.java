package com.hfstack.royalcrown;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class RCAdvancements {
    private RCAdvancements() {}

    public static final ResourceLocation ROOT      = rl("root");
    public static final ResourceLocation ADVISOR   = rl("advisor_spawn");
    public static final ResourceLocation ACCEPTED  = rl("accepted");
    public static final ResourceLocation CROWNED   = rl("crowned");

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(ModMain.MODID, path);
    }

    /** Concede todas as pendências de um advancement para o jogador. */
    public static void grant(ServerPlayer sp, ResourceLocation id) {
        if (sp == null) return;
        Advancement adv = sp.server.getAdvancements().getAdvancement(id);
        if (adv == null) return;
        AdvancementProgress prog = sp.getAdvancements().getOrStartProgress(adv);
        for (String c : prog.getRemainingCriteria()) {
            sp.getAdvancements().award(adv, c);
        }
    }
}
