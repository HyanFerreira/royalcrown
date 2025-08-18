package com.hfstack.royalcrown;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

public class CrownItem extends ArmorItem {
    public CrownItem(Properties props) {
        super(ArmorMaterials.GOLD, Type.HELMET, props);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        // Como é capacete, sempre layer_1
        return ModMain.MODID + ":textures/models/armor/crown_layer_1.png";
    }
}
