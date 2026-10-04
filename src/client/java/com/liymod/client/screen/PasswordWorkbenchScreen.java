package com.liymod.client.screen;

import com.liymod.client.gui.LoliGui;
import com.liymod.menu.PasswordWorkbenchMenu;
import com.liymod.network.PasswordUpdatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Original 3x3 password workbench presentation with server-authoritative submission. */
public final class PasswordWorkbenchScreen extends AbstractContainerScreen<PasswordWorkbenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("liymod", "textures/gui/container/password_crafting_table.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private EditBox passwordBox;
    private boolean submitted;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock errorText;

    public PasswordWorkbenchScreen(PasswordWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
        titleLabelX = 28;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 102;
    }

    @Override
    protected void init() {
        String previousPassword = passwordBox == null ? menu.getPassword() : passwordBox.getValue();
        super.init();
        titleText = LoliGui.text(font, title, imageWidth - 36, 1);
        errorText = LoliGui.text(font, Component.translatable("gui.liymod.password.no_match"), 81, 1);
        passwordBox = new EditBox(
                font,
                leftPos + 29,
                topPos + 18,
                75,
                16,
                Component.translatable("gui.liymod.password"));
        passwordBox.setMaxLength(PasswordUpdatePayload.MAX_CODE_POINTS);
        passwordBox.setTextColor(0xFFFFFFFF);
        passwordBox.setValue(previousPassword);
        addRenderableWidget(passwordBox);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.password.done"),
                        button -> submitPassword())
                .bounds(leftPos + 114, topPos + 16, 30, 20)
                .build());
        setInitialFocus(passwordBox);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
            submitPassword();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        super.renderBackground(graphics, mouseX, mouseY, deltaTicks);
        graphics.blit(
                RenderType::guiTextured,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        super.render(graphics, mouseX, mouseY, deltaTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        titleText.draw(graphics, font, 28, titleLabelY, TEXT_COLOR, mouseX, mouseY, leftPos, topPos);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, false);
        graphics.drawString(font, Component.translatable("container.crafting"), 28, 36, TEXT_COLOR, false);
        if (submitted && menu.getResultSlots().getItem(0).isEmpty()) {
            errorText.draw(graphics, font, 87, 86, ERROR_COLOR, mouseX, mouseY, leftPos, topPos);
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) { }

    private void submitPassword() {
        if (passwordBox == null) {
            return;
        }
        String sanitized = PasswordUpdatePayload.sanitize(passwordBox.getValue());
        passwordBox.setValue(sanitized);
        ClientPlayNetworking.send(new PasswordUpdatePayload(sanitized));
        submitted = true;
    }
}
