package com.liymod.client.screen;

import com.liymod.menu.FinalEffectMenu;
import com.liymod.network.LoliEffectUpdatePayload;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;

/** Client editor for the final pickaxe's server-authoritative status-effect list. */
public final class FinalEffectScreen extends AbstractContainerScreen<FinalEffectMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("liymod", "textures/gui/loli_pickaxe_config.png");
    private static final int PANEL_HEIGHT = 165;
    private static final int MAX_LEVEL = 32;
    private static final int MAX_ENTRIES = 64;
    private static final int TEXT_COLOR = 0xFF404040;
    private static final int ERROR_COLOR = 0xFFB02020;

    private final Registry<MobEffect> effectRegistry;
    private final List<Identifier> availableEffects = new ArrayList<>();
    private final Map<Identifier, Integer> originalEffects = new LinkedHashMap<>();
    private final Map<Identifier, Integer> draftEffects = new LinkedHashMap<>();

    private int effectIndex;
    private int selectedLevel = 1;
    private boolean entryLimitReached;
    private Button previousButton;
    private Button nextButton;
    private Button levelDownButton;
    private Button levelUpButton;
    private Button addButton;
    private Button removeButton;

    public FinalEffectScreen(FinalEffectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 220, PANEL_HEIGHT);
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
        previousButton = addRenderableWidget(Button.builder(
                        Component.literal("<"),
                        button -> changeSelection(-1))
                .bounds(leftPos + 10, topPos + 31, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.previous")))
                .build());
        nextButton = addRenderableWidget(Button.builder(
                        Component.literal(">"),
                        button -> changeSelection(1))
                .bounds(leftPos + 190, topPos + 31, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.liymod.config.next")))
                .build());
        levelDownButton = addRenderableWidget(Button.builder(
                        Component.literal("-"),
                        button -> changeLevel(-1))
                .bounds(leftPos + 40, topPos + 57, 20, 20)
                .build());
        levelUpButton = addRenderableWidget(Button.builder(
                        Component.literal("+"),
                        button -> changeLevel(1))
                .bounds(leftPos + 160, topPos + 57, 20, 20)
                .build());
        addButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.potion.add"),
                        button -> addOrUpdateSelected())
                .bounds(leftPos + 15, topPos + 84, 90, 20)
                .build());
        removeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.potion.remove"),
                        button -> removeSelected())
                .bounds(leftPos + 115, topPos + 84, 90, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.liymod.potion.save"),
                        button -> saveAndClose())
                .bounds(leftPos + 15, topPos + 137, 190, 20)
                .build());
        refreshControls();
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
        graphics.centeredText(font, title, imageWidth / 2, 7, TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.potion.available"),
                imageWidth / 2,
                20,
                TEXT_COLOR);

        Identifier selected = selectedEffect();
        if (selected == null) {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.liymod.potion.empty"),
                    imageWidth / 2,
                    38,
                    TEXT_COLOR);
        } else {
            graphics.centeredText(font, displayName(selected), imageWidth / 2, 38, TEXT_COLOR);
        }

        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.potion.level").append(": " + selectedLevel),
                imageWidth / 2,
                63,
                TEXT_COLOR);
        graphics.centeredText(
                font,
                Component.translatable("gui.liymod.potion.selected")
                        .append(": " + draftEffects.size() + " / " + MAX_ENTRIES),
                imageWidth / 2,
                111,
                TEXT_COLOR);

        if (entryLimitReached) {
            graphics.centeredText(
                    font,
                    Component.literal("64 / 64"),
                    imageWidth / 2,
                    124,
                    ERROR_COLOR);
        } else if (selected != null && draftEffects.containsKey(selected)) {
            graphics.centeredText(
                    font,
                    Component.translatable("gui.liymod.potion.level")
                            .append(": " + draftEffects.get(selected)),
                    imageWidth / 2,
                    124,
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
        Identifier selected = selectedEffect();
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
        Identifier selected = selectedEffect();
        if (selected != null) {
            draftEffects.remove(selected);
            selectedLevel = 0;
            entryLimitReached = false;
        }
        refreshControls();
    }

    private void saveAndClose() {
        Set<Identifier> changedIds = new LinkedHashSet<>(originalEffects.keySet());
        changedIds.addAll(draftEffects.keySet());
        for (Identifier id : changedIds) {
            int originalLevel = originalEffects.getOrDefault(id, 0);
            int draftLevel = draftEffects.getOrDefault(id, 0);
            if (originalLevel != draftLevel) {
                ClientPlayNetworking.send(new LoliEffectUpdatePayload(id.toString(), draftLevel));
            }
        }
        onClose();
    }

    private void loadSelectedLevel() {
        Identifier selected = selectedEffect();
        selectedLevel = selected == null ? 0 : draftEffects.getOrDefault(selected, 1);
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

    private Identifier selectedEffect() {
        return availableEffects.isEmpty()
                ? null
                : availableEffects.get(Math.clamp(effectIndex, 0, availableEffects.size() - 1));
    }

    private Component displayName(Identifier id) {
        return effectRegistry.get(id)
                .<Component>map(holder -> holder.value().getDisplayName())
                .orElseGet(() -> Component.literal(id.toString()));
    }
}
