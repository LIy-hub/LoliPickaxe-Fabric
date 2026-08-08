package com.liymod.client;

import com.liymod.network.ModNetwork;
import com.liymod.storage.LoliStorageData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class BlacklistScreen extends Screen {
    private final ItemStack tool; private EditBox id;
    public BlacklistScreen(ItemStack tool) { super(Component.translatable("container.liymod.loli_blacklist")); this.tool = tool; }
    @Override protected void init() {
        id = new EditBox(font, width / 2 - 100, 42, 200, 20, Component.literal("minecraft:item")); id.setMaxLength(128); id.setValue("minecraft:cobblestone"); addRenderableWidget(id);
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.enchantment.add"), b -> update(true)).bounds(width / 2 - 100, 68, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.enchantment.remove"), b -> update(false)).bounds(width / 2 + 4, 68, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 50, height - 30, 100, 20).build());
    }
    private void update(boolean add) { ResourceLocation value = ResourceLocation.tryParse(id.getValue()); if (value == null) return; LoliStorageData.setBlacklisted(tool, value, add); ModNetwork.CHANNEL.sendToServer(new ModNetwork.BlacklistPacket(value.toString(), add)); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        int y = 100; for (ResourceLocation entry : LoliStorageData.blacklist(tool)) { graphics.drawString(font, entry.toString(), width / 2 - 100, y, 0xFFF5F5F5, false); y += 11; if (y > height - 45) break; }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
