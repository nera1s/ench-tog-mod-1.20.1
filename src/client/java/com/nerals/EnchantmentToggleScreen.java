package com.nerals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantmentToggleScreen extends Screen {
    private static final String DISABLED_ENCHANTMENTS_TAG = "ench-tag:disabled_enchantments";
    private ItemStack itemStack;
    private final List<EnchantmentEntry> enchantmentEntries;
    private int scrollOffset = 0;
    private int targetScrollOffset = 0;
    private float scrollVelocity = 0.0f;
    private int maxScroll;
    private static final int ENTRY_HEIGHT = 25;
    private int visibleEntries; // Теперь это будет динамическая переменная
    private static final int SCROLL_AMOUNT = 25;
    private static final int BUTTON_HEIGHT = 20;

    public EnchantmentToggleScreen() {
        super(Text.translatable("screen.ench-tag.title"));
        enchantmentEntries = new ArrayList<>();
    }

    private static class EnchantmentEntry {
        Enchantment enchantment;
        int level;
        boolean isActive;
        boolean isPending;
        ButtonWidget button;

        EnchantmentEntry(Enchantment enchantment, int level, boolean isActive) {
            this.enchantment = enchantment;
            this.level = level;
            this.isActive = isActive;
            this.isPending = false;
        }
    }

    @Override
    protected void init() {
        this.clearChildren();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            this.close();
            return;
        }

        itemStack = client.player.getMainHandStack();
        Map<Enchantment, Integer> activeEnchantments = EnchantmentHelper.get(itemStack);
        Map<Enchantment, Integer> disabledEnchantments = getDisabledEnchantments(itemStack);

        enchantmentEntries.clear();

        for (Map.Entry<Enchantment, Integer> entry : activeEnchantments.entrySet()) {
            enchantmentEntries.add(new EnchantmentEntry(entry.getKey(), entry.getValue(), true));
        }

        for (Map.Entry<Enchantment, Integer> entry : disabledEnchantments.entrySet()) {
            enchantmentEntries.add(new EnchantmentEntry(entry.getKey(), entry.getValue(), false));
        }

        // Динамически вычисляем количество видимых элементов
        int topMargin = 30; // Отступ сверху (для заголовка)
        int bottomMargin = 40; // Отступ снизу (для кнопки "Save & Exit")
        int availableHeight = this.height - topMargin - bottomMargin; // Доступная высота для списка
        this.visibleEntries = Math.max(1, availableHeight / ENTRY_HEIGHT); // Вычисляем количество видимых элементов

        maxScroll = Math.max(0, (enchantmentEntries.size() - visibleEntries) * ENTRY_HEIGHT);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
        targetScrollOffset = scrollOffset;

        int buttonX = 220;
        int y = 30;

        if (enchantmentEntries.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("screen.ench-tag.no_enchantments"),
                    button -> this.close()
            ).dimensions(this.width / 2 - 100, this.height / 2, 200, 20).build());
            return;
        }

        for (int i = 0; i < enchantmentEntries.size(); i++) {
            EnchantmentEntry entry = enchantmentEntries.get(i);
            int entryY = 30 + i * ENTRY_HEIGHT - scrollOffset;
            // Синхронизируем диапазон с render
            if (entryY >= 30 - ENTRY_HEIGHT && entryY <= 30 + visibleEntries * ENTRY_HEIGHT) {
                if (!entry.enchantment.isCursed()) {
                    entry.button = ButtonWidget.builder(
                            Text.translatable(entry.isActive ? "screen.ench-tag.button.off" : "screen.ench-tag.button.on"),
                            button -> {
                                entry.isPending = true;
                                entry.isActive = !entry.isActive;
                                entry.button.setMessage(Text.translatable(entry.isActive ? "screen.ench-tag.button.off" : "screen.ench-tag.button.on"));
                                EnchantNetworking.sendToggleEnchantmentPacket(entry.isActive, entry.enchantment, entry.level);
                            }
                    ).dimensions(buttonX, entryY, 50, 20).build();
                    addDrawableChild(entry.button);
                }
            }
        }

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.ench-tag.save_and_exit"),
                button -> this.close()
        ).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    private Map<Enchantment, Integer> getDisabledEnchantments(ItemStack stack) {
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        context.drawText(this.textRenderer, Text.translatable("screen.ench-tag.title"), 10, 10, 0xFFFFFF, true);

        MinecraftClient client = MinecraftClient.getInstance();
        float time = client.getTickDelta() + (System.currentTimeMillis() % 1000) / 1000.0f;
        float blinkAlpha = (float) (Math.sin(time * 2 * Math.PI) * 0.5 + 0.5);
        int alpha = (int) (blinkAlpha * 255);
        int blinkColor = (alpha << 24) | 0xFF0000;

        for (int i = 0; i < enchantmentEntries.size(); i++) {
            EnchantmentEntry entry = enchantmentEntries.get(i);
            int entryY = 30 + i * ENTRY_HEIGHT - scrollOffset;
            if (entryY >= 30 - ENTRY_HEIGHT && entryY <= 30 + visibleEntries * ENTRY_HEIGHT) {
                int color = entry.enchantment.isCursed() ? 0x808080 : (entry.isPending ? 0x808080 : (entry.isActive ? 0x00FF00 : 0xFF0000));
                String name = entry.enchantment.getName(entry.level).getString();
                int textY = entryY + (BUTTON_HEIGHT - this.textRenderer.fontHeight) / 2;
                context.drawText(this.textRenderer, Text.literal(name), 10, textY, color, true);

                if (entry.enchantment.isCursed()) {
                    Text cannotToggleText = Text.translatable("screen.ench-tag.cannot_toggle").setStyle(Style.EMPTY.withBold(true).withColor(Formatting.RED));
                    int textX = 10 + this.textRenderer.getWidth(name) + 10;
                    context.drawText(this.textRenderer, cannotToggleText, textX, textY, 0xFF0000, true);

                    int symbolXLeft = textX - 10;
                    int symbolXRight = textX + this.textRenderer.getWidth(cannotToggleText) + 5;
                    context.drawText(this.textRenderer, Text.literal("~"), symbolXLeft, textY, blinkColor, true);
                    context.drawText(this.textRenderer, Text.literal("~"), symbolXRight, textY, blinkColor, true);
                }
            }
        }

        if (enchantmentEntries.size() > visibleEntries) {
            int scrollBarHeight = (int) ((float) visibleEntries / enchantmentEntries.size() * (visibleEntries * ENTRY_HEIGHT));
            int scrollBarY = 30 + (int) ((float) scrollOffset / maxScroll * (visibleEntries * ENTRY_HEIGHT - scrollBarHeight));
            context.fill(this.width - 10, 30, this.width - 5, 30 + visibleEntries * ENTRY_HEIGHT, 0xFF555555);
            context.fill(this.width - 10, scrollBarY, this.width - 5, scrollBarY + scrollBarHeight, 0xFFAAAAAA);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();

        if (scrollOffset != targetScrollOffset) {
            int diff = targetScrollOffset - scrollOffset;
            scrollVelocity = diff * 0.3f;
            scrollOffset += (int) scrollVelocity;
            scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
            init();
        }

        for (EnchantmentEntry entry : enchantmentEntries) {
            if (entry.isPending) {
                Map<Enchantment, Integer> activeEnchantments = EnchantmentHelper.get(itemStack);
                boolean isActuallyActive = activeEnchantments.containsKey(entry.enchantment);
                if (isActuallyActive != entry.isActive) {
                    continue;
                }
                entry.isPending = false;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (enchantmentEntries.size() > visibleEntries) {
            targetScrollOffset = MathHelper.clamp(
                    targetScrollOffset - (int) (amount * SCROLL_AMOUNT),
                    0,
                    maxScroll
            );
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}