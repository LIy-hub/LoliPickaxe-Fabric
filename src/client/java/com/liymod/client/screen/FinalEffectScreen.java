package com.liymod.client.screen;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.menu.FinalEffectMenu;
import com.liymod.network.LoliEffectUpdatePayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;

/** Client editor for the final pickaxe's server-authoritative status-effect list. */
public final class FinalEffectScreen extends AbstractLoliEditorScreen<FinalEffectMenu> {
    private static final int MAX_LEVEL = 32;
    private static final int MAX_ENTRIES = 64;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final Registry<MobEffect> effectRegistry;
    private final List<ResourceLocation> availableEffects = new ArrayList<>();
    private final Map<ResourceLocation, Integer> originalEffects = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> draftEffects = new LinkedHashMap<>();

    private int effectIndex;
    private int selectedLevel = 1;
    private boolean entryLimitReached;
    private Button previousButton;
    private Button nextButton;
    private Button levelDownButton;
    private Button levelUpButton;
    private Button addButton;
    private Button removeButton;
    private LoliGui.TextBlock titleText;
    private LoliGui.TextBlock selectionText;
    private int selectionRowHeight;

    public FinalEffectScreen(FinalEffectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        effectRegistry = inventory.player.level().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        availableEffects.addAll(effectRegistry.keySet());
        availableEffects.sort((left, right) -> left.toString().compareTo(right.toString()));
        originalEffects.putAll(menu.getEffects());
        draftEffects.putAll(originalEffects);
        loadSelectedLevel();
    }

    @Override
    protected void init() {
        super.init();
        int contentWidth = EditorLayout.panelWidth(width, 300) - 20;
        titleText = LoliGui.text(font, title, contentWidth, 2);
        selectionRowHeight = Math.max(20, availableEffects.stream()
                .mapToInt(id -> LoliGui.text(font, displayName(id), contentWidth - 52, 2).height())
                .max().orElse(font.lineHeight));
        placePanel(EditorLayout.create(width, height, 300, titleText.height(), font.lineHeight, selectionRowHeight,
                20, 20, font.lineHeight, font.lineHeight, 20));
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
                .bounds(leftPos + 10, topPos + layout.row(3), 20, 20)
                .build());
        levelUpButton = addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        button -> changeLevel(1))
                .bounds(leftPos + layout.width() - 30, topPos + layout.row(3), 20, 20)
                .build());
        addButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.potion.add"),
                        button -> addOrUpdateSelected())
                .bounds(leftPos + 10, topPos + layout.row(4), (layout.contentWidth() - 6) / 2, 20)
                .build());
        removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.potion.remove"),
                        button -> removeSelected())
                .bounds(leftPos + 16 + (layout.contentWidth() - 6) / 2, topPos + layout.row(4), (layout.contentWidth() - 6) / 2, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.potion.save"),
                        button -> saveAndClose())
                .bounds(leftPos + 10, topPos + layout.row(7), layout.contentWidth(), 20)
                .build());
        refreshControls();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawLabel(graphics, titleText, 10, layout.row(0), TEXT_COLOR, mouseX, mouseY);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.potion.available"),
                layout.width() / 2,
                layout.row(1),
                TEXT_COLOR);

        ResourceLocation selected = selectedEffect();
        drawLabel(graphics, selectionText, 36, layout.row(2) + (selectionRowHeight - selectionText.height()) / 2,
                TEXT_COLOR, mouseX, mouseY);

        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.potion.level").append(": " + selectedLevel),
                layout.width() / 2,
                layout.row(3) + (20 - font.lineHeight) / 2,
                TEXT_COLOR);
        LoliGui.centeredText(graphics,
                font,
                Component.translatable("gui.liymod.potion.selected")
                        .append(": " + draftEffects.size() + " / " + MAX_ENTRIES),
                layout.width() / 2,
                layout.row(5),
                TEXT_COLOR);

        if (entryLimitReached) {
            LoliGui.centeredText(graphics,
                    font,
                    Component.literal("64 / 64"),
                    layout.width() / 2,
                    layout.row(6),
                    ERROR_COLOR);
        } else if (selected != null && draftEffects.containsKey(selected)) {
            LoliGui.centeredText(graphics,
                    font,
                    Component.translatable("gui.liymod.potion.level")
                            .append(": " + draftEffects.get(selected)),
                    layout.width() / 2,
                    layout.row(6),
                    TEXT_COLOR);
        }
    }

    private void changeSelection(int delta) {
        if (availableEffects.isEmpty()) {
            return;
        }
        effectIndex = Math.floorMod(effectIndex + Integer.signum(delta), availableEffects.size());
        entryLimitReached = false;
        loadSelectedLevel();
        refreshControls();
    }

    private void changeLevel(int delta) {
        selectedLevel = Math.clamp(selectedLevel + Integer.signum(delta), 0, MAX_LEVEL);
        entryLimitReached = false;
        refreshControls();
    }

    private void addOrUpdateSelected() {
        ResourceLocation selected = selectedEffect();
        if (selected == null) {
            return;
        }
        if (selectedLevel == 0) {
            draftEffects.remove(selected);
            entryLimitReached = false;
        } else if (draftEffects.containsKey(selected) || draftEffects.size() < MAX_ENTRIES) {
            draftEffects.put(selected, selectedLevel);
            entryLimitReached = false;
        } else {
            entryLimitReached = true;
        }
        refreshControls();
    }

    private void removeSelected() {
        ResourceLocation selected = selectedEffect();
        if (selected != null) {
            draftEffects.remove(selected);
            selectedLevel = 0;
            entryLimitReached = false;
        }
        refreshControls();
    }

    private void saveAndClose() {
        Set<ResourceLocation> changedIds = new LinkedHashSet<>(originalEffects.keySet());
        changedIds.addAll(draftEffects.keySet());
        for (ResourceLocation id : changedIds) {
            int originalLevel = originalEffects.getOrDefault(id, 0);
            int draftLevel = draftEffects.getOrDefault(id, 0);
            if (originalLevel != draftLevel) {
                ClientPlayNetworking.send(new LoliEffectUpdatePayload(id.toString(), draftLevel));
            }
        }
        onClose();
    }

    private void loadSelectedLevel() {
        ResourceLocation selected = selectedEffect();
        selectedLevel = selected == null ? 0 : draftEffects.getOrDefault(selected, 1);
        if (layout != null) {
            updateSelectionText();
        }
    }

    private void refreshControls() {
        boolean hasSelection = selectedEffect() != null;
        previousButton.active = hasSelection;
        nextButton.active = hasSelection;
        levelDownButton.active = hasSelection && selectedLevel > 0;
        levelUpButton.active = hasSelection && selectedLevel < MAX_LEVEL;
        addButton.active = hasSelection;
        removeButton.active = hasSelection && draftEffects.containsKey(selectedEffect());
    }

    private ResourceLocation selectedEffect() {
        return availableEffects.isEmpty()
                ? null
                : availableEffects.get(Math.clamp(effectIndex, 0, availableEffects.size() - 1));
    }

    private Component displayName(ResourceLocation id) {
        return effectRegistry.get(id)
                .<Component>map(holder -> holder.value().getDisplayName())
                .orElseGet(() -> Component.literal(id.toString()));
    }

    private void updateSelectionText() {
        ResourceLocation selected = selectedEffect();
        selectionText = LoliGui.text(font, selected == null
                ? Component.translatable("gui.liymod.potion.empty") : displayName(selected),
                layout.contentWidth() - 52, 2);
    }
}
