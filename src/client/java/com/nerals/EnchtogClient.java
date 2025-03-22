package com.nerals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class EnchtogClient implements ClientModInitializer {
	private static KeyBinding openScreenKey;
	private static final String CONTROLLER_APPLIED_TAG = "ench-tag:controller_applied";

	@Override
	public void onInitializeClient() {
		openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.ench-tag.open_screen",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_G,
				"category.ench-tag"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null) return;

			// Проверка нажатия клавиши G
			while (openScreenKey.wasPressed()) {
				ItemStack heldItem = client.player.getMainHandStack();
				if (!heldItem.isEmpty() && !EnchantmentHelper.get(heldItem).isEmpty()) {
					if (hasControllerApplied(heldItem)) {
						// Если контроллер применён, открываем интерфейс
						client.setScreen(new EnchantmentToggleScreen());
					} else {
						// Если контроллер не применён, показываем сообщение
						client.player.sendMessage(
								Text.translatable("message.ench-tag.controller_not_applied").formatted(Formatting.RED),
								true
						);
					}
				}
			}
		});

		TooltipHandler.register();
	}

	private static boolean hasControllerApplied(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		return nbt != null && nbt.getBoolean(CONTROLLER_APPLIED_TAG);
	}
}