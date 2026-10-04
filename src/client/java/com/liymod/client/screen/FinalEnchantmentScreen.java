package com.liymod.client.screen;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.menu.FinalEnchantmentMenu;
import com.liymod.network.LoliEnchantmentUpdatePayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.enchantment.Enchantment;

/** Client editor for the final pickaxe's server-authoritative enchantment list. */
public final class FinalEnchantmentScreen extends AbstractLoliEditorScreen<FinalEnchantmentMenu> {
    private static final int MAX_LEVEL = 32768;
    private static final int MAX_ENTRIES = 64;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final Registry<Enchantment> enchantmentRegistry;
    private final List<ResourceLocation> availableEnchantments = new ArrayList<>();
    private final Map<ResourceLocation, Integer> originalEnchantments = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> draftEnchantments = new LinkedHashMap<>();

    private int enchantmentIndex;
    private int selectedLevel = 1;
    private boolean entryLimitReached;
    private Button previousButton;
    private Button nextButton;
    private Button levelDownButton;
    private Button levelUpButton;
    private Button addButton;
    private Button removeButton;
    private EditBox levelBox;
    private boolean invalidLevel;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock selectionText;
    private LoliGui.TextBlock errorText;
    private int selectionRowHeight;

    public FinalEnchantmentScreen(FinalEnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        enchantmentRegistry = inventory.player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        availableEnchantments.addAll(enchantmentRegistry.keySet());
        availableEnchantments.sort((left, right) -> left.toString().compareTo(right.toString()));
        originalEnchantments.putAll(menu.getEnchantments());
        draftEnchantments.putAll(originalEnchantments);
        loadSelectedLevel();
    }

    @Override
    protected void init() {
        String previousInput = levelBox == null ? Integer.toString(selectedLevel) : levelBox.getValue();
        super.init();
        int contentWidth = EditorLayout.panelWidth(width, 300) - 20;
        titleText = LoliGui.text(font, title, contentWidth, 2);
        errorText = LoliGui.text(font, Component.translatable("gui.liymod.enchantment.invalid_level", MAX_LEVEL), contentWidth, 2);
        selectionRowHeight = Math.max(20, availableEnchantments.stream()
                .mapToInt(id -> LoliGui.text(font, displayName(id), contentWidth - 52, 2).height())
                .max().orElse(font.lineHeight));
        placePanel(EditorLayout.create(width, height, 300, titleText.height(), font.lineHeight, selectionRowHeight,
                font.lineHeight, 20, 20, font.lineHeight, Math.max(font.lineHeight, errorText.height()), 20));
        updateSelectionText();
        previousButton = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> changeSelection(-1))
                .bounds(leftPos + 10, topPos + layout.row(2), 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.previous")))
                .build());
        nextButton = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> changeSelection(1))
                .bounds(leftPos + layout.width() - 30, topPos + layout.row(2), 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.next")))
                .build());
        levelDownButton = addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        button -> changeLevel(-1))
                .bounds(leftPos + 10, topPos + layout.row(4), 20, 20)
                .build());
        levelUpButton = addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        button -> changeLevel(1))
                .bounds(leftPos + layout.width() - 30, topPos + layout.row(4), 20, 20)
                .build());
        levelBox = new EditBox(
                font,
                leftPos + 36,
                topPos + layout.row(4),
                layout.contentWidth() - 52,
                20,
                Component.translatable("gui.liymod.enchantment.level"));
        levelBox.setMaxLength(5);
        levelBox.setValue(previousInput);
        addRenderableWidget(levelBox);
        addButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.enchantment.add"),
                        button -> addOrUpdateSelected())
                .bounds(leftPos + 10, topPos + layout.row(5), (layout.contentWidth() - 6) / 2, 20)
                .build());
        removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.enchantment.remove"),
                        button -> removeSelected())
                .bounds(leftPos + 16 + (layout.contentWidth() - 6) / 2, topPos + layout.row(5), (layout.contentWidth() - 6) / 2, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.enchantment.save"),
                        button -> saveAndClose())
                .bounds(leftPos + 10, topPos + layout.row(8), layout.contentWidth(), 20)
                .build());
        // Install the callback only after every control it updates exists.
        levelBox.setResponder(this::readLevelInput);
        readLevelInput(previousInput);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabel(graphics, titleText, 10, layout.row(0), TEXT_COLOR, mouseX, mouseY);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.enchantment.available"),
                layout.width() / 2,
                layout.row(1),
                TEXT_COLOR);

        ResourceLocation selected = selectedEnchantment();
        drawLabel(graphics, selectionText, 36, layout.row(2) + (selectionRowHeight - selectionText.height()) / 2,
                TEXT_COLOR, mouseX, mouseY);

        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.enchantment.level"),
                layout.width() / 2,
                layout.row(3),
                TEXT_COLOR);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.enchantment.selected")
                        .append(": " + draftEnchantments.size() + " / " + MAX_ENTRIES),
                layout.width() / 2,
                layout.row(6),
                TEXT_COLOR);

        if (invalidLevel) {
            drawLabel(graphics, errorText, 10, layout.row(7), ERROR_COLOR, mouseX, mouseY);
        } else if (entryLimitReached) {
            LoliGui.centeredText(graphics,
                    font,
                    Component.literal("64 / 64"),
                    layout.width() / 2,
                    layout.row(7),
                    ERROR_COLOR);
        } else if (selected != null && draftEnchantments.containsKey(selected)) {
            LoliGui.centeredText(graphics,
                    font,
                    Component.translatable("gui.liymod.enchantment.level")
                            .append(": " + draftEnchantments.get(selected)),
                    layout.width() / 2,
                    layout.row(7),
                    TEXT_COLOR);
        }
    }

    private void changeSelection(int delta) {
        if (availableEnchantments.isEmpty()) {
            return;
        }
        enchantmentIndex = Math.floorMod(enchantmentIndex + Integer.signum(delta), availableEnchantments.size());
        entryLimitReached = false;
        loadSelectedLevel();
        refreshControls();
    }

    private void changeLevel(int delta) {
        selectedLevel = Math.clamp(selectedLevel + Integer.signum(delta), 0, MAX_LEVEL);
        levelBox.setValue(Integer.toString(selectedLevel));
        entryLimitReached = false;
        refreshControls();
    }

    private void addOrUpdateSelected() {
        if (invalidLevel) {
            return;
        }
        ResourceLocation selected = selectedEnchantment();
        if (selected == null) {
            return;
        }
        if (selectedLevel == 0) {
            draftEnchantments.remove(selected);
            entryLimitReached = false;
        } else if (draftEnchantments.containsKey(selected) || draftEnchantments.size() < MAX_ENTRIES) {
            draftEnchantments.put(selected, selectedLevel);
            entryLimitReached = false;
        } else {
            entryLimitReached = true;
        }
        refreshControls();
    }

    private void removeSelected() {
        ResourceLocation selected = selectedEnchantment();
        if (selected != null) {
            draftEnchantments.remove(selected);
            selectedLevel = 0;
            levelBox.setValue("0");
            entryLimitReached = false;
        }
        refreshControls();
    }

    private void saveAndClose() {
        if (invalidLevel) {
            return;
        }
        Set<ResourceLocation> changedIds = new LinkedHashSet<>(originalEnchantments.keySet());
        changedIds.addAll(draftEnchantments.keySet());
        for (ResourceLocation id : changedIds) {
            int originalLevel = originalEnchantments.getOrDefault(id, 0);
            int draftLevel = draftEnchantments.getOrDefault(id, 0);
            if (originalLevel != draftLevel) {
                ClientPlayNetworking.send(new LoliEnchantmentUpdatePayload(id.toString(), draftLevel));
            }
        }
        onClose();
    }

    private void loadSelectedLevel() {
        ResourceLocation selected = selectedEnchantment();
        selectedLevel = selected == null ? 0 : draftEnchantments.getOrDefault(selected, 1);
        if (levelBox != null) {
            levelBox.setValue(Integer.toString(selectedLevel));
        }
        if (layout != null) {
            updateSelectionText();
        }
    }

    private void readLevelInput(String encoded) {
        invalidLevel = true;
        try {
            int parsed = Integer.parseInt(encoded);
            if (parsed >= 0 && parsed <= MAX_LEVEL) {
                selectedLevel = parsed;
                invalidLevel = false;
                entryLimitReached = false;
            }
        } catch (NumberFormatException ignored) {
            // An incomplete/invalid field keeps the draft unchanged and disables applying it.
        }
        refreshControls();
    }

    private void refreshControls() {
        boolean hasSelection = selectedEnchantment() != null;
        previousButton.active = hasSelection;
        nextButton.active = hasSelection;
        levelDownButton.active = hasSelection && selectedLevel > 0;
        levelUpButton.active = hasSelection && selectedLevel < MAX_LEVEL;
        addButton.active = hasSelection && !invalidLevel;
        removeButton.active = hasSelection && draftEnchantments.containsKey(selectedEnchantment());
    }

    private ResourceLocation selectedEnchantment() {
        return availableEnchantments.isEmpty()
                ? null
                : availableEnchantments.get(Math.clamp(enchantmentIndex, 0, availableEnchantments.size() - 1));
    }

    private Component displayName(ResourceLocation id) {
        return enchantmentRegistry.getHolder(id)
                .<Component>map(holder -> holder.value().description())
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private void updateSelectionText() {
        ResourceLocation selected = selectedEnchantment();
        selectionText = LoliGui.text(font, selected == null
                ? Component.translatable("gui.liymod.enchantment.empty") : displayName(selected),
                layout.contentWidth() - 52, 2);
    }
}
