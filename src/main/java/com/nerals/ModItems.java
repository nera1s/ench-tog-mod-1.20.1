package com.nerals;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item ENCHANTMENT_CONTROLLER = new EnchantmentControllerItem(new Item.Settings().maxCount(1));

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier("ench-tog", "enchantment_controller"), ENCHANTMENT_CONTROLLER);
    }
}