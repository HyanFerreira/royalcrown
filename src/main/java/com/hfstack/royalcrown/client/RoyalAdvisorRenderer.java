package com.hfstack.royalcrown.client;

import com.hfstack.royalcrown.ModMain;
import com.hfstack.royalcrown.RoyalAdvisorEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RoyalAdvisorRenderer extends MobRenderer<RoyalAdvisorEntity, VillagerModel<RoyalAdvisorEntity>> {

    // Use a textura vanilla de aldeão, ou troque para a sua:
    // private static final ResourceLocation TEX =
    //         new ResourceLocation("minecraft", "textures/entity/villager/villager.png");

    // Para textura própria:
    private static final ResourceLocation TEX =
            new ResourceLocation(ModMain.MODID, "textures/entity/royal_advisor.png");

    public RoyalAdvisorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(RoyalAdvisorEntity entity) {
        return TEX;
    }
}
