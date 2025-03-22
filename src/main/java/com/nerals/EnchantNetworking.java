package com.nerals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

public class EnchantNetworking {
    public static final Identifier TOGGLE_ENCHANTMENT_PACKET = new Identifier("ench-tag", "toggle_enchantment");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_ENCHANTMENT_PACKET, (server, player, handler, buf, responseSender) -> {
            boolean enable = buf.readBoolean();
            String enchantmentId = buf.readString(32767);
            int level = buf.readInt();

            server.execute(() -> {
                ItemStack stack = player.getStackInHand(Hand.MAIN_HAND);
                Enchantment enchantment = Registries.ENCHANTMENT.get(new Identifier(enchantmentId));
                if (enchantment != null) {
                    if (enable) {
                        enableEnchantment(stack, enchantment, level);
                    } else {
                        disableEnchantment(stack, enchantment, level);
                    }
                    player.setStackInHand(Hand.MAIN_HAND, stack);
                }
            });
        });
    }

    @Environment(EnvType.CLIENT)
    public static void sendToggleEnchantmentPacket(boolean enable, Enchantment enchantment, int level) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(enable);
        Identifier enchantmentId = Registries.ENCHANTMENT.getId(enchantment);
        if (enchantmentId == null) {
            return;
        }
        buf.writeString(enchantmentId.toString());
        buf.writeInt(level);

        try {
            Class<?> clientPlayNetworkingClass = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            java.lang.reflect.Method sendMethod = clientPlayNetworkingClass.getMethod("send", Identifier.class, PacketByteBuf.class);
            sendMethod.invoke(null, TOGGLE_ENCHANTMENT_PACKET, buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String DISABLED_ENCHANTMENTS_TAG = "ench-tag:disabled_enchantments";

    private static void disableEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        Map<Enchantment, Integer> activeEnchantments = EnchantmentHelper.get(stack);
        activeEnchantments.remove(enchantment);
        EnchantmentHelper.set(activeEnchantments, stack);

        Map<Enchantment, Integer> disabledEnchantments = getDisabledEnchantments(stack);
        disabledEnchantments.put(enchantment, level);

        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList disabledList = new NbtList();
        for (Map.Entry<Enchantment, Integer> entry : disabledEnchantments.entrySet()) {
            NbtCompound enchantmentCompound = new NbtCompound();
            Identifier id = Registries.ENCHANTMENT.getId(entry.getKey());
            if (id != null) {
                enchantmentCompound.putString("id", id.toString());
                enchantmentCompound.putShort("lvl", entry.getValue().shortValue());
                disabledList.add(enchantmentCompound);
            }
        }
        nbt.put(DISABLED_ENCHANTMENTS_TAG, disabledList);
        stack.setNbt(nbt);
    }

    private static void enableEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        Map<Enchantment, Integer> disabledEnchantments = getDisabledEnchantments(stack);
        disabledEnchantments.remove(enchantment);

        NbtCompound nbt = stack.getOrCreateNbt();
        if (disabledEnchantments.isEmpty()) {
            nbt.remove(DISABLED_ENCHANTMENTS_TAG);
        } else {
            NbtList disabledList = new NbtList();
            for (Map.Entry<Enchantment, Integer> entry : disabledEnchantments.entrySet()) {
                NbtCompound enchantmentCompound = new NbtCompound();
                Identifier id = Registries.ENCHANTMENT.getId(entry.getKey());
                if (id != null) {
                    enchantmentCompound.putString("id", id.toString());
                    enchantmentCompound.putShort("lvl", entry.getValue().shortValue());
                    disabledList.add(enchantmentCompound);
                }
            }
            nbt.put(DISABLED_ENCHANTMENTS_TAG, disabledList);
        }
        stack.setNbt(nbt);

        Map<Enchantment, Integer> activeEnchantments = EnchantmentHelper.get(stack);
        activeEnchantments.put(enchantment, level);
        EnchantmentHelper.set(activeEnchantments, stack);
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
}