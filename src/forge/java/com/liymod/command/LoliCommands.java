package com.liymod.command;

import com.liymod.config.LoliServerConfig;
import com.liymod.combat.LoliLegacyExecutionPolicy;
import com.liymod.safe.SafeEffect;
import com.liymod.safe.SafeEffectService;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class LoliCommands {
    private LoliCommands() { }
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("loli");
        root.requires(source -> source.hasPermission(2));
        root.then(Commands.literal("reload").executes(context -> { LoliServerConfig.load(); context.getSource().sendSuccess(() -> Component.literal("LoliPickaxe config reloaded"), true); return 1; }));
        root.then(Commands.literal("list").executes(context -> { LoliServerConfig.values().forEach((key, value) -> context.getSource().sendSuccess(() -> Component.literal(key + "=" + value), false)); return LoliServerConfig.values().size(); }));
        root.then(Commands.literal("get").then(Commands.argument("key", StringArgumentType.word()).executes(context -> { String key = StringArgumentType.getString(context, "key"); String value = LoliServerConfig.get(key); if (value.isEmpty()) return 0; context.getSource().sendSuccess(() -> Component.literal(key + "=" + value), false); return 1; })));
        root.then(Commands.literal("set").then(Commands.argument("key", StringArgumentType.word()).then(Commands.argument("value", StringArgumentType.greedyString()).executes(context -> {
                    String key = StringArgumentType.getString(context, "key"), value = StringArgumentType.getString(context, "value"); boolean changed = LoliServerConfig.set(key, value);
                    context.getSource().sendSuccess(() -> Component.literal(changed ? key + "=" + LoliServerConfig.get(key) : "Invalid LoliPickaxe option/value"), true); return changed ? 1 : 0;
                }))));
        root.then(Commands.literal("playerlist").then(Commands.argument("list", StringArgumentType.word())
                .then(Commands.literal("list").executes(context -> {
                    String key = listKey(StringArgumentType.getString(context, "list")); if (key == null) return 0;
                    var entries = LoliLegacyExecutionPolicy.entries(key);
                    context.getSource().sendSuccess(() -> Component.literal(key + "=" + String.join(",", entries)), false);
                    return entries.size();
                }))
                .then(Commands.literal("add").then(Commands.argument("player", StringArgumentType.word()).executes(context -> {
                    String key = listKey(StringArgumentType.getString(context, "list"));
                    return key != null && LoliLegacyExecutionPolicy.addEntry(key, StringArgumentType.getString(context, "player")) ? 1 : 0;
                })))
                .then(Commands.literal("remove").then(Commands.argument("player", StringArgumentType.word()).executes(context -> {
                    String key = listKey(StringArgumentType.getString(context, "list"));
                    return key != null && LoliLegacyExecutionPolicy.removeEntry(key, StringArgumentType.getString(context, "player")) ? 1 : 0;
                })))));
        event.getDispatcher().register(root);
        event.getDispatcher().register(Commands.literal("loliattack").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("effect", StringArgumentType.word()).executes(context -> {
                    if (!LoliServerConfig.bool("safe_attack_command")) return 0;
                    SafeEffect effect = switch (StringArgumentType.getString(context, "effect")) {
                        case "blue_screen" -> SafeEffect.BLUE_SCREEN; case "exit" -> SafeEffect.EXIT; case "fail_respond" -> SafeEffect.FAIL_RESPOND; default -> null;
                    };
                    return effect != null && SafeEffectService.apply(EntityArgument.getPlayer(context, "player"), effect) ? 1 : 0;
                }))));
    }

    private static String listKey(String value) {
        return switch (value) {
            case "reincarnation", "reincarnation_list" -> "reincarnation_list";
            case "soul_redemption", "soul_redemption_list" -> "soul_redemption_list";
            case "soul_whitelist", "soul_redemption_whitelist" -> "soul_redemption_whitelist";
            default -> null;
        };
    }
}
