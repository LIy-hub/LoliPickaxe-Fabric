package com.liymod.client;

import com.liymod.LiyMod;
import com.liymod.config.FinalToolSettings;
import com.liymod.menu.FinalToolMenu;
import com.liymod.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Server-authoritative final-pickaxe editor shared by the N/M/P/K menus. */
public final class FinalToolScreen extends AbstractContainerScreen<FinalToolMenu> {
    private static final ResourceLocation CONFIG = new ResourceLocation(LiyMod.MOD_ID, "textures/gui/loli_pickaxe_config.png");
    private static final ResourceLocation TELEPORT = new ResourceLocation(LiyMod.MOD_ID, "textures/gui/loli_pickaxe_space_folding.png");
    private EditBox idBox;
    private EditBox levelBox;
    private EditBox dimensionBox;
    private EditBox xBox;
    private EditBox yBox;
    private EditBox zBox;
    private EditBox kickMessageBox;
    private int configPage;

    public FinalToolScreen(FinalToolMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 196;
    }

    @Override protected void init() {
        super.init();
        switch (menu.mode()) {
            case CONFIG -> initConfig();
            case ENCHANTMENT -> initRegistryEditor(true);
            case EFFECT -> initRegistryEditor(false);
            case TELEPORT -> initTeleport();
        }
    }

    private void initConfig() {
        int x = leftPos + 18, y = topPos + 32, w = 204;
        if (configPage == 0) {
            addRenderableWidget(Button.builder(configLabel("mining_radius", Integer.toString(FinalToolSettings.radius(menu.tool()))), b -> {
                int next = (FinalToolSettings.radius(menu.tool()) + 1) % 6;
                sendSetting("mining_radius", Integer.toString(next)); b.setMessage(configLabel("mining_radius", Integer.toString(next)));
            }).bounds(x, y, w, 20).build());
            y += 24;
            y = toggle(x, y, w, "stop_on_liquid", FinalToolSettings.stopOnLiquid(menu.tool()));
            y = toggle(x, y, w, "auto_accept", FinalToolSettings.autoAccept(menu.tool()));
            y = toggle(x, y, w, "auto_furnace", FinalToolSettings.autoFurnace(menu.tool()));
            toggle(x, y, w, "mandatory_drop", FinalToolSettings.mandatoryDrop(menu.tool()));
        } else if (configPage == 1) {
            y = toggle(x, y, w, "thorns", FinalToolSettings.thorns(menu.tool()));
            y = toggle(x, y, w, "auto_kill_range_entity", FinalToolSettings.autoKill(menu.tool()));
            addRenderableWidget(Button.builder(configLabel("auto_kill_range", Integer.toString(FinalToolSettings.autoKillRange(menu.tool()))), b -> {
                int next = FinalToolSettings.autoKillRange(menu.tool()) % 10 + 1;
                sendSetting("auto_kill_range", Integer.toString(next));
                b.setMessage(configLabel("auto_kill_range", Integer.toString(next)));
            }).bounds(x, y, w, 20).build());
            y += 24;
            y = toggle(x, y, w, "target_friendly_entities", FinalToolSettings.targetFriendly(menu.tool()));
            toggle(x, y, w, "target_all_entities", FinalToolSettings.targetAll(menu.tool()));
        } else if (configPage == 2) {
            y = toggle(x, y, w, "force_remove", FinalToolSettings.forceRemove(menu.tool()));
            y = toggle(x, y, w, "clear_inventory", FinalToolSettings.clearInventory(menu.tool()));
            y = toggle(x, y, w, "drop_equipment", FinalToolSettings.dropEquipment(menu.tool()));
            y = toggle(x, y, w, "kick_player", FinalToolSettings.kickPlayer(menu.tool()));
            toggle(x, y, w, "reincarnation", FinalToolSettings.reincarnation(menu.tool()));
        } else {
            y = toggle(x, y, w, "soul_redemption", FinalToolSettings.soulRedemption(menu.tool()));
            kickMessageBox = new EditBox(font, x, y, 142, 20, Component.translatable("config.liymod.loli.kick_message"));
            kickMessageBox.setMaxLength(160); kickMessageBox.setValue(FinalToolSettings.kickMessage(menu.tool())); addRenderableWidget(kickMessageBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.liymod.config.save"), b -> sendSetting("kick_message", kickMessageBox.getValue()))
                    .bounds(x + 146, y, 58, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { configPage = (configPage + 3) % 4; rebuildWidgets(); })
                .bounds(x, topPos + 164, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal((configPage + 1) + "/4"), b -> { })
                .bounds(x + 32, topPos + 164, 140, 20).build()).active = false;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { configPage = (configPage + 1) % 4; rebuildWidgets(); })
                .bounds(x + 176, topPos + 164, 28, 20).build());
    }

    private int toggle(int x, int y, int width, String key, boolean initial) {
        final boolean[] value = {initial};
        addRenderableWidget(Button.builder(configLabel(key, Boolean.toString(initial)), b -> {
            value[0] = !value[0]; sendSetting(key, Boolean.toString(value[0])); b.setMessage(configLabel(key, Boolean.toString(value[0])));
        }).bounds(x, y, width, 20).build());
        return y + 24;
    }

    private void initRegistryEditor(boolean enchantment) {
        int x = leftPos + 22, y = topPos + 48;
        idBox = new EditBox(font, x, y, 196, 20, Component.literal("registry id"));
        idBox.setMaxLength(128);
        idBox.setValue(enchantment ? "minecraft:sharpness" : "minecraft:regeneration");
        addRenderableWidget(idBox);
        levelBox = new EditBox(font, x, y + 30, 80, 20, Component.literal("level"));
        levelBox.setMaxLength(5);
        levelBox.setValue(enchantment ? "32768" : "1");
        addRenderableWidget(levelBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.config.save"), b -> sendRegistry(enchantment, false))
                .bounds(x + 88, y + 30, 62, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.enchantment.remove"), b -> sendRegistry(enchantment, true))
                .bounds(x + 154, y + 30, 42, 20).build());
    }

    private void sendRegistry(boolean enchantment, boolean remove) {
        if (idBox == null || levelBox == null) return;
        int level;
        try { level = remove ? 0 : Integer.parseInt(levelBox.getValue()); } catch (NumberFormatException ignored) { return; }
        if (enchantment) ModNetwork.CHANNEL.sendToServer(new ModNetwork.EnchantmentPacket(idBox.getValue(), Math.max(0, Math.min(32768, level))));
        else ModNetwork.CHANNEL.sendToServer(new ModNetwork.EffectPacket(idBox.getValue(), Math.max(0, Math.min(32, level))));
    }

    private void initTeleport() {
        int x = leftPos + 24, y = topPos + 42;
        dimensionBox = edit(x, y, 192, 128, minecraft.player == null ? "minecraft:overworld" : minecraft.player.level().dimension().location().toString());
        xBox = edit(x, y + 29, 60, 24, "0");
        yBox = edit(x + 66, y + 29, 60, 24, "0");
        zBox = edit(x + 132, y + 29, 60, 24, "0");
        addRenderableWidget(Button.builder(Component.translatable("gui.liymod.space_folding.teleport"), b -> {
            try {
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.TeleportPacket(dimensionBox.getValue(),
                        Double.parseDouble(xBox.getValue()), Double.parseDouble(yBox.getValue()), Double.parseDouble(zBox.getValue())));
            } catch (NumberFormatException ignored) { }
        }).bounds(x, y + 62, 192, 20).build());
    }

    private EditBox edit(int x, int y, int width, int max, String value) {
        EditBox box = new EditBox(font, x, y, width, 20, Component.empty());
        box.setMaxLength(max); box.setValue(value); addRenderableWidget(box); return box;
    }

    private void sendSetting(String key, String value) {
        FinalToolSettings.set(menu.tool(), key, value);
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.SettingPacket(key, value));
    }

    private static Component configLabel(String key, String value) {
        return Component.translatable("config.liymod.loli." + key).append(": " + value);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = menu.mode() == FinalToolMenu.Mode.TELEPORT ? TELEPORT : CONFIG;
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, 0xFFF5F5F5, false);
        if (menu.mode() == FinalToolMenu.Mode.ENCHANTMENT) {
            graphics.drawString(font, Component.translatable("gui.liymod.enchantment.level_limit", 32768), 22, 34, 0xFFFFFFFF, false);
        } else if (menu.mode() == FinalToolMenu.Mode.EFFECT) {
            graphics.drawString(font, Component.translatable("gui.liymod.potion.level"), 22, 34, 0xFFFFFFFF, false);
        } else if (menu.mode() == FinalToolMenu.Mode.TELEPORT) {
            graphics.drawString(font, Component.translatable("gui.liymod.space_folding.relative"), 24, 29, 0xFFFFFFFF, false);
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
