package com.hfstack.royalcrown;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
// imports novos
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistry {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModMain.MODID);

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModMain.MODID);

    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ModMain.MODID);

    // === ITEM: Coroa ===
    public static final RegistryObject<Item> CROWN = ITEMS.register("crown",
            () -> new CrownItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // dentro da classe ModRegistry:
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModMain.MODID);

    // Entidade do Conselheiro Real
    public static final RegistryObject<EntityType<RoyalAdvisorEntity>> ROYAL_ADVISOR =
            ENTITIES.register("royal_advisor", () ->
                    EntityType.Builder.<RoyalAdvisorEntity>of(RoyalAdvisorEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)   // tamanho de aldeão
                            .build(ModMain.MODID + ":royal_advisor")
            );
}
