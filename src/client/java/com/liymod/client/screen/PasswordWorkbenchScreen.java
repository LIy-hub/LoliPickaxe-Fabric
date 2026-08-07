package com.liymod.client.screen;

import com.liymod.menu.PasswordWorkbenchMenu;
import com.liymod.network.PasswordUpdatePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Original 3x3 password workbench presentation with server-authoritative submission. */
public final class PasswordWorkbenchScreen extends AbstractContainerScreen<PasswordWorkbenchMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("liymod", "textures/gui/container/password_crafting_table.png");
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private EditBox passwordBox;
    private boolean submitted;

    public PasswordWorkbenchScreen(PasswordWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 196);
        titleLabelX = 28;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 102;
    }

    @Override
    protected void init() {
        super.init();
        passwordBox = new EditBox(
                font,
                leftPos + 29,
                topPos + 18,
                75,
                16,
                Component.translatable("gui.liymod.password"));
        passwordBox.setMaxLength(PasswordUpdatePayload.MAX_CODE_POINTS);
        passwordBox.setTextColor(0xFFFFFFFF);
        passwordBox.setValue(menu.getPassword());
        addRenderableWidget(passwordBox);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.password.done"),
                        button -> submitPassword())
                .bounds(leftPos + 114, topPos + 16, 30, 20)
                .build());
        setInitialFocus(passwordBox);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            submitPassword();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        super.extractBackground(graphics, mouseX, mouseY, deltaTicks);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("container.crafting"), 28, 36, TEXT_COLOR, false);
        if (submitted && menu.getResultSlots().getItem(0).isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable("gui.liymod.password.no_match"),
                    84,
                    89,
                    ERROR_COLOR,
                    false);
        }
    }

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
