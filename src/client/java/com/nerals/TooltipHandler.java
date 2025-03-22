package com.nerals;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TooltipHandler {
    private static final String DISABLED_ENCHANTMENTS_TAG = "ench-tag:disabled_enchantments";
    private static final String CONTROLLER_APPLIED_TAG = "ench-tag:controller_applied";

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            // Проверяем, применен ли контроллер зачарований
            if (hasControllerApplied(stack)) {
                // Добавляем строку "Контроллер зачарований применен" в начало подсказки
                lines.add(1, Text.translatable("tooltip.ench-tag.controller_applied").formatted(Formatting.YELLOW));
            }

            // Получаем активные и отключенные зачарования
            Map<Enchantment, Integer> activeEnchantments = EnchantmentHelper.get(stack);
            Map<Enchantment, Integer> disabledEnchantments = getDisabledEnchantments(stack);

            // Если есть отключенные зачарования, добавляем их в подсказку
            if (!disabledEnchantments.isEmpty()) {
                // Находим индекс, где заканчиваются активные зачарования
                int enchantmentEndIndex = 1; // Пропускаем первую строку (название предмета)
                if (hasControllerApplied(stack)) {
                    enchantmentEndIndex++; // Пропускаем строку "Контроллер зачарований применен"
                }
                for (int i = 1; i < lines.size(); i++) {
                    Text line = lines.get(i);
                    if (!isEnchantmentLine(line, activeEnchantments)) {
                        enchantmentEndIndex = i;
                        break;
                    }
                }

                // Добавляем отключенные зачарования после активных
                for (Map.Entry<Enchantment, Integer> entry : disabledEnchantments.entrySet()) {
                    Enchantment enchantment = entry.getKey();
                    int level = entry.getValue();
                    MutableText enchantmentText = enchantment.getName(level).copy();
                    // Добавляем метку <disabled> с разными цветами
                    enchantmentText.append(" ")
                            .append(Text.literal("<").formatted(Formatting.GOLD))
                            .append(Text.translatable("enchantment.disabled").formatted(Formatting.WHITE))
                            .append(Text.literal(">").formatted(Formatting.GOLD));
                    lines.add(enchantmentEndIndex, enchantmentText);
                    enchantmentEndIndex++;
                }
            }
        });
    }

    private static Map<Enchantment, Integer> getDisabledEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> disabledEnchantments = new HashMap<>();
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(DISABLED_ENCHANTMENTS_TAG, NbtElement.LIST_TYPE)) {
            return disabledEnchantments;
        }

        NbtList disabledList = nbt.getList(DISABLED_ENCHANTMENTS_TAG, NbtElement.COMPOUND_TYPE);
        for (NbtElement element : disabledList) {
            NbtCompound enchantmentCompound = (NbtCompound) element;
            String id = enchantmentCompound.getString("id");
            int level = enchantmentCompound.getShort("lvl");
            Enchantment enchantment = Registries.ENCHANTMENT.get(new Identifier(id));
            if (enchantment != null) {
                disabledEnchantments.put(enchantment, level);
            }
        }
        return disabledEnchantments;
    }

    private static boolean isEnchantmentLine(Text line, Map<Enchantment, Integer> activeEnchantments) {
        for (Map.Entry<Enchantment, Integer> entry : activeEnchantments.entrySet()) {
            String enchantmentName = entry.getKey().getName(entry.getValue()).getString();
            if (line.getString().contains(enchantmentName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasControllerApplied(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(CONTROLLER_APPLIED_TAG);
    }
}