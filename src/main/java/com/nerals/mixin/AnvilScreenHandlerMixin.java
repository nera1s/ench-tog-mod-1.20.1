package com.nerals.mixin;

import com.nerals.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {
    private static final String CONTROLLER_APPLIED_TAG = "ench-tag:controller_applied";

    @Shadow private net.minecraft.screen.Property levelCost;

    public AnvilScreenHandlerMixin(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(method = "updateResult", at = @At("HEAD"), cancellable = true)
    private void onUpdateResult(CallbackInfo ci) {
        ItemStack left = this.input.getStack(0);
        ItemStack right = this.input.getStack(1);

        // Проверяем, что правый слот содержит "Контроллер зачарований"
        if (!left.isEmpty() && right.getItem() == ModItems.ENCHANTMENT_CONTROLLER) {
            // Создаем копию левого предмета
            ItemStack result = left.copy();
            // Добавляем тег controller_applied
            NbtCompound nbt = result.getNbt() != null ? result.getNbt().copy() : new NbtCompound();
            nbt.putBoolean(CONTROLLER_APPLIED_TAG, true);
            result.setNbt(nbt);
            // Устанавливаем результат в выходной слот
            this.output.setStack(0, result);
            // Устанавливаем стоимость в 10 уровень опыта
            this.levelCost.set(10);
            // Отменяем стандартную логику наковальни
            ci.cancel();
        }
    }
}