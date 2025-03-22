package com.nerals;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AnvilBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class AnvilHandler {
    private static final String CONTROLLER_APPLIED_TAG = "ench-tag:controller_applied";

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Проверяем, что игрок взаимодействует с наковальней
            if (!world.isClient && world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof AnvilBlock) {
                // Ждем, пока откроется экран наковальни
                player.getServer().execute(() -> {
                    ScreenHandler screenHandler = player.currentScreenHandler;
                    if (screenHandler instanceof AnvilScreenHandler anvilScreenHandler) {
                        // Добавляем слушатель для отслеживания изменений в слотах
                        screenHandler.addListener(new ScreenHandlerListener() {
                            @Override
                            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
                                // Проверяем, если игрок забрал предмет из выходного слота (слот 2)
                                if (slotId == 2 && !stack.isEmpty()) {
                                    ItemStack rightSlot = anvilScreenHandler.getSlot(1).getStack();
                                    if (rightSlot.getItem() == ModItems.ENCHANTMENT_CONTROLLER) {
                                        // Добавляем тег к предмету
                                        NbtCompound nbt = stack.getNbt() != null ? stack.getNbt().copy() : new NbtCompound();
                                        nbt.putBoolean(CONTROLLER_APPLIED_TAG, true);
                                        stack.setNbt(nbt);
                                        // Уменьшаем количество "Контроллера зачарований" (потребляем его)
                                        rightSlot.decrement(1);
                                        anvilScreenHandler.getSlot(1).setStack(rightSlot);
                                    }
                                }
                            }

                            @Override
                            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
                                // Не используем
                            }
                        });
                    }
                });
            }
            return ActionResult.PASS;
        });
    }
}