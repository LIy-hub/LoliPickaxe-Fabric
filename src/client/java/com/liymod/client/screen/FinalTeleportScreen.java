package com.liymod.client.screen;

import com.liymod.menu.FinalTeleportMenu;
import com.liymod.network.LoliTeleportPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Relative-coordinate teleport editor; the server remains authoritative for every safety check. */
public final class FinalTeleportScreen extends AbstractContainerScreen<FinalTeleportMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("liymod", "textures/gui/loli_pickaxe_space_folding.png");
    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_HEIGHT = 195;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private EditBox dimensionBox;
    private EditBox offsetXBox;
    private EditBox offsetYBox;
    private EditBox offsetZBox;
    private boolean invalidInput;

    public FinalTeleportScreen(FinalTeleportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        dimensionBox = new EditBox(
                font,
                leftPos + 15,
                topPos + 32,
                230,
                20,
                Component.translatable("gui.liymod.space_folding.dimension"));
        dimensionBox.setMaxLength(LoliTeleportPayload.MAX_ID_LENGTH);
        dimensionBox.setValue(menu.getCurrentDimension().toString());
        addRenderableWidget(dimensionBox);

        offsetXBox = coordinateBox(leftPos + 15, Component.translatable("gui.liymod.space_folding.x"));
        offsetYBox = coordinateBox(leftPos + 95, Component.translatable("gui.liymod.space_folding.y"));
        offsetZBox = coordinateBox(leftPos + 175, Component.translatable("gui.liymod.space_folding.z"));

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.space_folding.teleport"),
                        button -> requestTeleport())
                .bounds(leftPos + 15, topPos + 166, 110, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.space_folding.cancel"),
                        button -> onClose())
                .bounds(leftPos + 135, topPos + 166, 110, 20)
                .build());
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
                PANEL_WIDTH,
                PANEL_HEIGHT,
                90,
                50,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.centeredText(font, title, imageWidth / 2, 7, TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.space_folding.dimension"),
                imageWidth / 2,
                20,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable(
                        "gui.liymod.space_folding.current_dimension",
                        menu.getCurrentDimension().toString()),
                imageWidth / 2,
                56,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.space_folding.relative"),
                imageWidth / 2,
                69,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.space_folding.x"),
                50,
                82,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.space_folding.y"),
                130,
                82,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.space_folding.z"),
                210,
                82,
                TEXT_COLOR);
        graphics.textWithWordWrap(
                font,
                Component.translatable("gui.liymod.space_folding.server_authoritative"),
                15,
                119,
                230,
                TEXT_COLOR);
        if (invalidInput) {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.liymod.space_folding.invalid"),
                    imageWidth / 2,
                    150,
                    ERROR_COLOR);
        }
    }

    private EditBox coordinateBox(int x, Component label) {
        EditBox box = new EditBox(font, x, topPos + 92, 70, 20, label);
        box.setMaxLength(32);
        box.setValue("0");
        addRenderableWidget(box);
        return box;
    }

    private void requestTeleport() {
        Identifier dimension = Identifier.tryParse(dimensionBox.getValue().trim());
        Double offsetX = finiteDouble(offsetXBox.getValue());
        Double offsetY = finiteDouble(offsetYBox.getValue());
        Double offsetZ = finiteDouble(offsetZBox.getValue());
        if (dimension == null || offsetX == null || offsetY == null || offsetZ == null) {
            invalidInput = true;
            return;
        }
        invalidInput = false;
        ClientPlayNetworking.send(new LoliTeleportPayload(
                dimension.toString(),
                offsetX,
                offsetY,
                offsetZ));
        onClose();
    }

    private static Double finiteDouble(String encoded) {
        try {
            double value = Double.parseDouble(encoded.trim());
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
