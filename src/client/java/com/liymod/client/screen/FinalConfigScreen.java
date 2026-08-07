package com.liymod.client.screen;

import com.liymod.config.LoliConfigOption;
import com.liymod.menu.FinalConfigMenu;
import com.liymod.network.LoliItemSettingPayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Server-authoritative editor for the final pickaxe's per-item options. */
public final class FinalConfigScreen extends AbstractContainerScreen<FinalConfigMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("liymod", "textures/gui/loli_pickaxe_config.png");
    private static final int PANEL_HEIGHT = 120;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final Map<String, String> draftValues = new LinkedHashMap<>();
    private int optionIndex;
    private EditBox valueBox;
    private Button booleanButton;
    private Button previousButton;
    private Button nextButton;
    private boolean invalidValue;

    public FinalConfigScreen(FinalConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 220, 140);
        for (LoliConfigOption option : menu.getOptions()) {
            draftValues.put(option.id(), menu.getEncodedValue(option));
        }
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> changeOption(-1))
                .bounds(leftPos + 10, topPos + 28, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.previous")))
                .build());
        nextButton = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> changeOption(1))
                .bounds(leftPos + 190, topPos + 28, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.next")))
                .build());
        booleanButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        button -> toggleBoolean())
                .bounds(leftPos + 70, topPos + 58, 80, 20)
                .build());
        valueBox = new EditBox(
                font,
                leftPos + 30,
                topPos + 58,
                160,
                20,
                Component.translatable("gui.liymod.config.title"));
        valueBox.setMaxLength(LoliItemSettingPayload.MAX_VALUE_LENGTH);
        addRenderableWidget(valueBox);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.config.save"),
                        button -> saveAndClose())
                .bounds(leftPos + 10, topPos + 94, 200, 20)
                .build());
        showCurrentOption();
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
                PANEL_HEIGHT,
                imageWidth,
                PANEL_HEIGHT,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<LoliConfigOption> options = menu.getOptions();
        graphics.centeredText(font, title, imageWidth / 2, 7, TEXT_COLOR);
        if (options.isEmpty()) {
            return;
        }
        LoliConfigOption option = options.get(optionIndex);
        graphics.centeredText(
                font,
                Component.translatable(option.translationKey()),
                imageWidth / 2,
                32,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.config.page", optionIndex + 1, options.size()),
                imageWidth / 2,
                82,
                TEXT_COLOR);
        if (invalidValue) {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.liymod.config.invalid"),
                    imageWidth / 2,
                    121,
                    ERROR_COLOR);
        } else {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.liymod.config.server_authoritative"),
                    imageWidth / 2,
                    121,
                    TEXT_COLOR);
        }
    }

    private void changeOption(int delta) {
        if (!captureCurrentValue()) {
            return;
        }
        int size = menu.getOptions().size();
        if (size == 0) {
            return;
        }
        optionIndex = Math.floorMod(optionIndex + Integer.signum(delta), size);
        showCurrentOption();
    }

    private void toggleBoolean() {
        LoliConfigOption option = currentOption();
        if (option == null || option.type() != LoliConfigOption.ValueType.BOOLEAN) {
            return;
        }
        boolean next = !Boolean.parseBoolean(draftValues.getOrDefault(option.id(), "false"));
        draftValues.put(option.id(), Boolean.toString(next));
        updateBooleanMessage(next);
        invalidValue = false;
    }

    private void showCurrentOption() {
        LoliConfigOption option = currentOption();
        boolean hasOption = option != null;
        previousButton.active = hasOption;
        nextButton.active = hasOption;
        boolean isBoolean = hasOption && option.type() == LoliConfigOption.ValueType.BOOLEAN;
        booleanButton.visible = isBoolean;
        booleanButton.active = isBoolean;
        valueBox.setVisible(hasOption && !isBoolean);
        valueBox.setEditable(hasOption && !isBoolean);
        if (hasOption) {
            String encoded = draftValues.getOrDefault(option.id(), option.encode(option.defaultValue()));
            if (isBoolean) {
                updateBooleanMessage(Boolean.parseBoolean(encoded));
            } else {
                valueBox.setValue(encoded);
            }
        }
        invalidValue = false;
    }

    private boolean captureCurrentValue() {
        LoliConfigOption option = currentOption();
        if (option == null || option.type() == LoliConfigOption.ValueType.BOOLEAN) {
            invalidValue = false;
            return true;
        }
        try {
            draftValues.put(option.id(), option.encode(option.parse(valueBox.getValue())));
            invalidValue = false;
            return true;
        } catch (IllegalArgumentException exception) {
            invalidValue = true;
            return false;
        }
    }

    private void saveAndClose() {
        if (!captureCurrentValue()) {
            return;
        }
        for (Map.Entry<String, String> entry : draftValues.entrySet()) {
            ClientPlayNetworking.send(new LoliItemSettingPayload(entry.getKey(), entry.getValue()));
        }
        onClose();
    }

    private LoliConfigOption currentOption() {
        List<LoliConfigOption> options = menu.getOptions();
        return options.isEmpty() ? null : options.get(Math.clamp(optionIndex, 0, options.size() - 1));
    }

    private void updateBooleanMessage(boolean value) {
        booleanButton.setMessage(Component.translatable(
                value ? "gui.liymod.config.true" : "gui.liymod.config.false"));
    }
}
