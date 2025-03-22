package com.nerals;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    // Создаём вкладку
    public static final ItemGroup ENCH_TAG_GROUP = FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.ench-tog")) // Название вкладки (будет в lang-файле)
            .icon(() -> new ItemStack(ModItems.ENCHANTMENT_CONTROLLER)) // Иконка вкладки
            .entries((displayContext, entries) -> {
                // Добавляем предметы во вкладку
                entries.add(ModItems.ENCHANTMENT_CONTROLLER);
            })
            .build();

    public static void register() {
        // Регистрируем вкладку
        Registry.register(Registries.ITEM_GROUP, new Identifier("ench-tog", "ench_tag_group"), ENCH_TAG_GROUP);
    }
}