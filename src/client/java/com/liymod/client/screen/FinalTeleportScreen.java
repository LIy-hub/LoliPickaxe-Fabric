package com.liymod.client.screen;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.menu.FinalTeleportMenu;
import com.liymod.network.LoliTeleportPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Relative-coordinate teleport editor; the server remains authoritative for every safety check. */
public final class FinalTeleportScreen extends AbstractLoliEditorScreen<FinalTeleportMenu> {
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private EditBox dimensionBox;
    private EditBox offsetXBox;
    private EditBox offsetYBox;
    private EditBox offsetZBox;
    private boolean invalidInput;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock dimensionText;
    private LoliGui.TextBlock helpText;
    private LoliGui.TextBlock errorText;
    private int coordinateWidth;

    public FinalTeleportScreen(FinalTeleportMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        String dimension = dimensionBox == null ? menu.getCurrentDimension().toString() : dimensionBox.getValue();
        String x = offsetXBox == null ? "0" : offsetXBox.getValue();
        String y = offsetYBox == null ? "0" : offsetYBox.getValue();
        String z = offsetZBox == null ? "0" : offsetZBox.getValue();
        super.init();
        int contentWidth = EditorLayout.panelWidth(width, 320) - 20;
        titleText = LoliGui.text(font, title, contentWidth, 2);
        dimensionText = LoliGui.text(font, Component.translatable("gui.liymod.space_folding.current_dimension",
                menu.getCurrentDimension().toString()), contentWidth, 2);
        helpText = LoliGui.text(font, Component.translatable("gui.liymod.space_folding.server_authoritative"), contentWidth, 2);
        errorText = LoliGui.text(font, Component.translatable("gui.liymod.space_folding.invalid"), contentWidth, 2);
        placePanel(EditorLayout.create(width, height, 320, titleText.height(), font.lineHeight, 20,
                dimensionText.height(), font.lineHeight, font.lineHeight, 20, helpText.height(), errorText.height(), 20));
        coordinateWidth = (layout.contentWidth() - 12) / 3;
        dimensionBox = new EditBox(
                font,
                leftPos + 10,
                topPos + layout.row(2),
                layout.contentWidth(),
                20,
                Component.translatable("gui.liymod.space_folding.dimension"));
        dimensionBox.setMaxLength(LoliTeleportPayload.MAX_ID_LENGTH);
        dimensionBox.setValue(dimension);
        addRenderableWidget(dimensionBox);

        offsetXBox = coordinateBox(leftPos + 10, Component.translatable("gui.liymod.space_folding.x"), x);
        offsetYBox = coordinateBox(leftPos + 16 + coordinateWidth, Component.translatable("gui.liymod.space_folding.y"), y);
        offsetZBox = coordinateBox(leftPos + 22 + 2 * coordinateWidth, Component.translatable("gui.liymod.space_folding.z"), z);

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.space_folding.teleport"),
                        button -> requestTeleport())
                .bounds(leftPos + 10, topPos + layout.row(9), (layout.contentWidth() - 6) / 2, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.space_folding.cancel"),
                        button -> onClose())
                .bounds(leftPos + 16 + (layout.contentWidth() - 6) / 2, topPos + layout.row(9), (layout.contentWidth() - 6) / 2, 20)
                .build());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawLabel(graphics, titleText, 10, layout.row(0), TEXT_COLOR, mouseX, mouseY);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.space_folding.dimension"),
                layout.width() / 2,
                layout.row(1),
                TEXT_COLOR);
        drawLabel(graphics, dimensionText, 10, layout.row(3), TEXT_COLOR, mouseX, mouseY);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.space_folding.relative"),
                layout.width() / 2,
                layout.row(4),
                TEXT_COLOR);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.space_folding.x"),
                10 + coordinateWidth / 2,
                layout.row(5),
                TEXT_COLOR);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.space_folding.y"),
                16 + coordinateWidth + coordinateWidth / 2,
                layout.row(5),
                TEXT_COLOR);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.space_folding.z"),
                22 + 2 * coordinateWidth + coordinateWidth / 2,
                layout.row(5),
                TEXT_COLOR);
        drawLabel(graphics, helpText, 10, layout.row(7), TEXT_COLOR, mouseX, mouseY);
        if (invalidInput) {
            drawLabel(graphics, errorText, 10, layout.row(8), ERROR_COLOR, mouseX, mouseY);
        }
    }

    private EditBox coordinateBox(int x, Component label, String value) {
        EditBox box = new EditBox(font, x, topPos + layout.row(6), coordinateWidth, 20, label);
        box.setMaxLength(32);
        box.setValue(value);
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
