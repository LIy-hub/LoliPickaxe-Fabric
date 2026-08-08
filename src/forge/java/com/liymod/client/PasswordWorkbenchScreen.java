package com.liymod.client;

import com.liymod.LiyMod;
import com.liymod.menu.PasswordWorkbenchMenu;
import com.liymod.network.ModNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class PasswordWorkbenchScreen extends AbstractContainerScreen<PasswordWorkbenchMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(LiyMod.MOD_ID, "textures/gui/container/password_crafting_table.png");
    private EditBox password;
    private boolean submitted;
    public PasswordWorkbenchScreen(PasswordWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 176; imageHeight = 196; titleLabelX = 28; titleLabelY = 6; inventoryLabelY = 102;
    }
    @Override protected void init() {
        super.init();
        password = new EditBox(font, leftPos + 29, topPos + 18, 75, 16, Component.translatable("gui.liymod.password"));
        password.setMaxLength(64); password.setTextColor(0xFFFFFFFF); password.setValue(menu.password()); addRenderableWidget(password);
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.password.done"), b -> submit()).bounds(leftPos + 114, topPos + 16, 30, 20).build());
        setInitialFocus(password);
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) { submit(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    private void submit() {
        String value = ModNetwork.sanitizePassword(password.getValue()); password.setValue(value);
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.PasswordUpdatePacket(value)); submitted = true;
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) { graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256); }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFF5F5F5, true);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFF5F5F5, true);
        graphics.drawString(font, Component.translatable("container.crafting"), 28, 36, 0xFFF5F5F5, true);
        if (submitted && menu.result().getItem(0).isEmpty()) graphics.drawString(font, Component.translatable("gui.liymod.password.no_match"), 84, 89, 0xFFFF7070, true);
    }
}
