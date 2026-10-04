package com.liymod.client;

import com.liymod.client.gui.EditorLayout;
import com.liymod.client.gui.LoliGui;
import com.liymod.config.FinalToolSettings;
import com.liymod.menu.FinalToolMenu;
import com.liymod.network.ModNetwork;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;

/** Measured slotless editors; drafts survive both resize and configuration-page navigation. */
public final class FinalToolScreen extends AbstractContainerScreen<FinalToolMenu> {
    private static final String[][] CONFIG_KEYS={
        {"mining_radius","stop_on_liquid","auto_accept","auto_furnace"},
        {"thorns","auto_kill_range_entity","auto_kill_range","target_friendly_entities","target_all_entities"},
        {"force_remove","clear_inventory","drop_equipment","kick_player","reincarnation"},
        {"soul_redemption","kick_message"}
    };
    private final Map<String,String> drafts=new HashMap<>();
    private final Map<String,EditBox> fields=new HashMap<>();
    private EditorLayout layout;
    private LoliGui.TextBlock heading;
    private Component error=Component.empty();
    private int configPage;

    public FinalToolScreen(FinalToolMenu menu,Inventory inventory,Component title) { super(menu,inventory,title); }
    @Override protected void init() {
        fields.forEach((key,box)->drafts.put(key,box.getValue()));fields.clear();
        super.init();
        int preferred=260;
        if(menu.mode()==FinalToolMenu.Mode.CONFIG) {
            for(String key:CONFIG_KEYS[configPage]) preferred=Math.max(preferred,font.width(configLabel(key,value(key)))+28);
        }
        int panelWidth=EditorLayout.panelWidth(width,Math.min(340,preferred));
        heading=LoliGui.text(font,title,panelWidth-20,2);
        int rows=menu.mode()==FinalToolMenu.Mode.CONFIG?CONFIG_KEYS[configPage].length:3;
        int[] heights=new int[rows+3];heights[0]=heading.height();
        for(int i=1;i<=rows;i++) heights[i]=20;
        heights[rows+1]=20;heights[rows+2]=font.lineHeight*2;
        layout=EditorLayout.create(width,height,panelWidth,heights);
        imageWidth=layout.width();imageHeight=layout.height();leftPos=(width-imageWidth)/2;topPos=(height-imageHeight)/2;
        switch(menu.mode()) {
            case CONFIG -> initConfig();
            case ENCHANTMENT -> initRegistry(true);
            case EFFECT -> initRegistry(false);
            case TELEPORT -> initTeleport();
        }
    }
    private void initConfig() {
        int x=leftPos+10,w=layout.contentWidth();
        String[] keys=CONFIG_KEYS[configPage];
        for(int i=0;i<keys.length;i++) {
            String key=keys[i];int y=topPos+layout.row(i+1);
            if(key.equals("kick_message")) {
                int saveWidth=Math.max(48,font.width(Component.translatable("gui.liymod.config.save"))+12);
                edit(key,x,y,w-saveWidth-4,160,FinalToolSettings.kickMessage(menu.tool()));
                button(Component.translatable("gui.liymod.config.save"),x+w-saveWidth,y,saveWidth,b->sendSetting(key,fields.get(key).getValue()));
            } else button(configLabel(key,value(key)),x,y,w,b->{
                String next=key.equals("mining_radius")?Integer.toString((FinalToolSettings.radius(menu.tool())+1)%6):
                        key.equals("auto_kill_range")?Integer.toString(FinalToolSettings.autoKillRange(menu.tool())%10+1):Boolean.toString(!Boolean.parseBoolean(value(key)));
                sendSetting(key,next);setButtonLabel(b,configLabel(key,next));
            });
        }
        int y=topPos+layout.row(keys.length+1);
        button(Component.literal("<"),x,y,28,b->{configPage=(configPage+3)%4;rebuildWidgets();});
        button(Component.literal((configPage+1)+"/4"),x+32,y,w-64,b->{}).active=false;
        button(Component.literal(">"),x+w-28,y,28,b->{configPage=(configPage+1)%4;rebuildWidgets();});
    }
    private String value(String key) {
        return switch(key) {
            case "mining_radius" -> Integer.toString(FinalToolSettings.radius(menu.tool()));
            case "auto_kill_range" -> Integer.toString(FinalToolSettings.autoKillRange(menu.tool()));
            case "kick_message" -> FinalToolSettings.kickMessage(menu.tool());
            case "stop_on_liquid" -> Boolean.toString(FinalToolSettings.stopOnLiquid(menu.tool()));
            case "auto_accept" -> Boolean.toString(FinalToolSettings.autoAccept(menu.tool()));
            case "auto_furnace" -> Boolean.toString(FinalToolSettings.autoFurnace(menu.tool()));
            case "thorns" -> Boolean.toString(FinalToolSettings.thorns(menu.tool()));
            case "auto_kill_range_entity" -> Boolean.toString(FinalToolSettings.autoKill(menu.tool()));
            case "target_friendly_entities" -> Boolean.toString(FinalToolSettings.targetFriendly(menu.tool()));
            case "target_all_entities" -> Boolean.toString(FinalToolSettings.targetAll(menu.tool()));
            case "force_remove" -> Boolean.toString(FinalToolSettings.forceRemove(menu.tool()));
            case "clear_inventory" -> Boolean.toString(FinalToolSettings.clearInventory(menu.tool()));
            case "drop_equipment" -> Boolean.toString(FinalToolSettings.dropEquipment(menu.tool()));
            case "kick_player" -> Boolean.toString(FinalToolSettings.kickPlayer(menu.tool()));
            case "reincarnation" -> Boolean.toString(FinalToolSettings.reincarnation(menu.tool()));
            default -> Boolean.toString(FinalToolSettings.soulRedemption(menu.tool()));
        };
    }
    private void initRegistry(boolean enchantment) {
        int x=leftPos+10,w=layout.contentWidth();
        edit("id",x,topPos+layout.row(1),w,128,enchantment?"minecraft:sharpness":"minecraft:regeneration");
        int save=Math.max(50,font.width(Component.translatable("gui.liymod.config.save"))+12);
        int remove=Math.max(50,font.width(Component.translatable("gui.liymod.enchantment.remove"))+12);
        int y=topPos+layout.row(2),levelWidth=Math.max(24,w-save-remove-8);
        edit("level",x,y,levelWidth,6,enchantment?"32768":"1");
        button(Component.translatable("gui.liymod.config.save"),x+levelWidth+4,y,save,b->sendRegistry(enchantment,false));
        button(Component.translatable("gui.liymod.enchantment.remove"),x+w-remove,y,remove,b->sendRegistry(enchantment,true));
        button(Component.translatable("gui.done"),x,topPos+layout.row(4),w,b->onClose());
    }
    private void sendRegistry(boolean enchantment,boolean remove) {
        String encoded=fields.get("id").getValue().strip();ResourceLocation id=ResourceLocation.tryParse(encoded);
        int level;
        try { level=remove?0:Integer.parseInt(fields.get("level").getValue()); }
        catch(NumberFormatException invalid) { error=Component.translatable("gui.liymod.config.invalid");return; }
        int maximum=enchantment?32768:32;
        if(id==null || !(enchantment?BuiltInRegistries.ENCHANTMENT.containsKey(id):BuiltInRegistries.MOB_EFFECT.containsKey(id)) || level<0 || level>maximum) {
            error=Component.translatable("gui.liymod.config.invalid");return;
        }
        if(enchantment) ModNetwork.CHANNEL.sendToServer(new ModNetwork.EnchantmentPacket(encoded,level));
        else ModNetwork.CHANNEL.sendToServer(new ModNetwork.EffectPacket(encoded,level));
        error=Component.empty();
    }
    private void initTeleport() {
        int x=leftPos+10,w=layout.contentWidth();
        edit("dimension",x,topPos+layout.row(1),w,128,minecraft.player==null?"minecraft:overworld":minecraft.player.level().dimension().location().toString());
        int third=(w-8)/3,y=topPos+layout.row(2);
        edit("x",x,y,third,24,"0");edit("y",x+third+4,y,third,24,"0");edit("z",x+2*(third+4),y,w-2*(third+4),24,"0");
        button(Component.translatable("gui.liymod.space_folding.teleport"),x,topPos+layout.row(3),w,b->teleport());
        button(Component.translatable("gui.done"),x,topPos+layout.row(4),w,b->onClose());
    }
    private void teleport() {
        try {
            String dimension=fields.get("dimension").getValue().strip();
            double x=Double.parseDouble(fields.get("x").getValue()),y=Double.parseDouble(fields.get("y").getValue()),z=Double.parseDouble(fields.get("z").getValue());
            if(ResourceLocation.tryParse(dimension)==null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x)>30000000 || Math.abs(y)>30000000 || Math.abs(z)>30000000) throw new NumberFormatException();
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.TeleportPacket(dimension,x,y,z));error=Component.empty();
        } catch(NumberFormatException invalid) { error=Component.translatable("gui.liymod.space_folding.invalid"); }
    }
    private EditBox edit(String key,int x,int y,int w,int max,String initial) {
        EditBox box=new EditBox(font,x,y,w,20,Component.literal(key));box.setMaxLength(max);
        box.setValue(drafts.getOrDefault(key,initial));fields.put(key,box);addRenderableWidget(box);return box;
    }
    private Button button(Component label,int x,int y,int width,java.util.function.Consumer<Button> action) {
        Button button=addRenderableWidget(Button.builder(label,action::accept).bounds(x,y,width,20).build());setButtonLabel(button,label);return button;
    }
    private void setButtonLabel(Button button,Component label) {
        button.setTooltip(Tooltip.create(label));
        button.setMessage(font.width(label)<=button.getWidth()-8?label:Component.literal(font.plainSubstrByWidth(label.getString(),Math.max(1,button.getWidth()-font.width("…")-8))+"…"));
    }
    private void sendSetting(String key,String value) { FinalToolSettings.set(menu.tool(),key,value);ModNetwork.CHANNEL.sendToServer(new ModNetwork.SettingPacket(key,value)); }
    private static Component configLabel(String key,String value) { return Component.translatable("config.liymod.loli."+key).append(": "+value); }
    @Override protected boolean hasClickedOutside(double x,double y,int left,int top,int button) { return !layout.contains(x-left,y-top); }
    @Override protected void renderBg(GuiGraphics graphics,float delta,int x,int y) { LoliGui.panel(graphics,leftPos,topPos,imageWidth,imageHeight); }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        heading.draw(graphics,font,10,layout.row(0),LoliGui.TEXT_COLOR,mouseX,mouseY,leftPos,topPos);
        if(menu.mode()==FinalToolMenu.Mode.ENCHANTMENT || menu.mode()==FinalToolMenu.Mode.EFFECT) {
            Component label=menu.mode()==FinalToolMenu.Mode.ENCHANTMENT?Component.translatable("gui.liymod.enchantment.level_limit",32768):Component.translatable("gui.liymod.potion.level");
            LoliGui.text(font,label,layout.contentWidth(),2).draw(graphics,font,10,layout.row(3),LoliGui.TEXT_COLOR,mouseX,mouseY,leftPos,topPos);
        }
        LoliGui.text(font,error,layout.contentWidth(),2).draw(graphics,font,10,layout.row(layout.rowTops().size()-1),LoliGui.ERROR_COLOR,mouseX,mouseY,leftPos,topPos);
    }
    @Override public void render(GuiGraphics graphics,int x,int y,float delta) { renderBackground(graphics);super.render(graphics,x,y,delta);renderTooltip(graphics,x,y); }
}
