package com.liymod.client.screen;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.config.LoliConfigOption;
import com.liymod.item.LoliPickaxeItem;
import com.liymod.menu.FinalConfigMenu;
import com.liymod.network.LoliItemSettingPayload;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Server-authoritative editor for the final pickaxe's per-item options. */
public final class FinalConfigScreen extends AbstractLoliEditorScreen<FinalConfigMenu> {
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final Map<String, String> draftValues = new LinkedHashMap<>();
    private int optionIndex;
    private EditBox valueBox;
    private Button booleanButton;
    private Button previousButton;
    private Button nextButton;
    private boolean invalidValue;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock optionText;
    private LoliGui.TextBlock helpText;
    private LoliGui.TextBlock errorText;
    private int optionRowHeight;

    public FinalConfigScreen(FinalConfigMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        for (LoliConfigOption option : menu.getOptions()) {
            draftValues.put(option.id(), menu.getEncodedValue(option));
        }
    }

    @Override
    protected void init() {
        String previousValue = valueBox == null ? null : valueBox.getValue();
        boolean previousInvalid = invalidValue;
        super.init();
        int panelWidth = EditorLayout.panelWidth(width, 300);
        int contentWidth = panelWidth - 20;
        titleText = LoliGui.text(font, title, contentWidth, 2);
        helpText = LoliGui.text(font, Component.translatable("gui.liymod.config.server_authoritative"), contentWidth, 2);
        errorText = LoliGui.text(font, Component.translatable("gui.liymod.config.invalid"), contentWidth, 2);
        optionRowHeight = menu.getOptions().stream()
                .mapToInt(option -> LoliGui.text(font, Component.translatable(option.translationKey()), contentWidth - 52, 2).height())
                .max().orElse(font.lineHeight);
        optionRowHeight = Math.max(20, optionRowHeight);
        for (int radius : new int[]{0, 5}) {
            optionRowHeight = Math.max(optionRowHeight, LoliGui.text(font,
                    LoliPickaxeItem.miningModeDescription(radius), contentWidth - 52, 2).height());
        }
        placePanel(EditorLayout.create(width, height, 300, titleText.height(), optionRowHeight, 20,
                font.lineHeight, 20, Math.max(helpText.height(), errorText.height())));
        previousButton = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> changeOption(-1))
                .bounds(leftPos + 10, topPos + layout.row(1), 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.previous")))
                .build());
        nextButton = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> changeOption(1))
                .bounds(leftPos + layout.width() - 30, topPos + layout.row(1), 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.next")))
                .build());
        booleanButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        button -> toggleBoolean())
                .bounds(leftPos + (layout.width() - 100) / 2, topPos + layout.row(2), 100, 20)
                .build());
        valueBox = new EditBox(
                font,
                leftPos + 10,
                topPos + layout.row(2),
                layout.contentWidth(),
                20,
                Component.translatable("gui.liymod.config.title"));
        valueBox.setMaxLength(LoliItemSettingPayload.MAX_VALUE_LENGTH);
        valueBox.setResponder(ignored -> updateOptionText());
        addRenderableWidget(valueBox);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.config.save"),
                        button -> saveAndClose())
                .bounds(leftPos + 10, topPos + layout.row(4), layout.contentWidth(), 20)
                .build());
        showCurrentOption();
        if (previousValue != null && valueBox.isVisible()) {
            valueBox.setValue(previousValue);
        }
        invalidValue = previousInvalid;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabel(graphics, titleText, 10, layout.row(0), TEXT_COLOR, mouseX, mouseY);
        drawLabel(graphics, optionText, 36, layout.row(1) + (optionRowHeight - optionText.height()) / 2,
                TEXT_COLOR, mouseX, mouseY);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.config.page", menu.getOptions().isEmpty() ? 0 : optionIndex + 1, menu.getOptions().size()),
                layout.width() / 2,
                layout.row(3),
                TEXT_COLOR);
        drawLabel(graphics, invalidValue ? errorText : helpText, 10, layout.row(5),
                invalidValue ? ERROR_COLOR : TEXT_COLOR, mouseX, mouseY);
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
        updateOptionText();
        invalidValue = false;
    }

    private void updateOptionText() {
        LoliConfigOption option = currentOption();
        Component label = option == null ? Component.empty() : Component.translatable(option.translationKey());
        if (option == LoliConfigOption.MINING_RADIUS && valueBox != null) {
            try {
                label = LoliPickaxeItem.miningModeDescription((Integer) option.parse(valueBox.getValue()));
            } catch (IllegalArgumentException ignored) {
                // Keep the option name while the user enters an incomplete number.
            }
        }
        optionText = LoliGui.text(font, label, layout.contentWidth() - 52, 2);
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
