package com.liymod.command;

import com.liymod.config.LoliConfigOption;
import com.liymod.config.LoliServerConfig;
import com.liymod.combat.LoliLegacyExecutionPolicy;
import com.liymod.safe.SafeTntEffect;
import com.liymod.safe.SafeTntEffectService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Operator-only modern equivalents of the legacy /loli and /loliattack commands. */
public final class LoliCommands {
    private LoliCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("loli")
                .requires(LoliCommands::isAdministrator)
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource())))
                .then(Commands.literal("get")
                        .then(Commands.argument("option", StringArgumentType.word())
                                .suggests((context, builder) -> suggestOptions(builder))
                                .executes(context -> get(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "option")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("option", StringArgumentType.word())
                                .suggests((context, builder) -> suggestOptions(builder))
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .executes(context -> set(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "option"),
                                                StringArgumentType.getString(context, "value")
                                        )))))
                .then(Commands.literal("playerlist")
                        .then(Commands.argument("list", StringArgumentType.word())
                                .suggests((context, builder) -> suggestPlayerLists(builder))
                                .then(Commands.literal("list")
                                        .executes(context -> listPlayers(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "list")
                                        )))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .executes(context -> addPlayer(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "list"),
                                                        StringArgumentType.getString(context, "player")
                                                ))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", StringArgumentType.word())
                                                .executes(context -> removePlayer(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "list"),
                                                        StringArgumentType.getString(context, "player")
                                                )))))));

        dispatcher.register(Commands.literal("loliattack")
                .requires(LoliCommands::isAdministrator)
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("effect", StringArgumentType.word())
                                .suggests((context, builder) -> suggestEffects(builder))
                                .executes(context -> safeAttack(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"),
                                        StringArgumentType.getString(context, "effect")
                                )))));
    }

    private static int reload(CommandSourceStack source) {
        LoliServerConfig.reload();
        source.sendSuccess(() -> Component.literal("LiyMod Loli configuration reloaded."), true);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        for (LoliConfigOption option : LoliConfigOption.values()) {
            source.sendSuccess(() -> Component.literal(
                    option.id() + "=" + option.encode(LoliServerConfig.get(option))
            ), false);
        }
        return LoliConfigOption.values().length;
    }

    private static int get(CommandSourceStack source, String encodedOption) {
        LoliConfigOption option = LoliConfigOption.byId(encodedOption).orElse(null);
        if (option == null) {
            source.sendFailure(Component.literal("Unknown Loli option: " + encodedOption));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                option.id() + "=" + option.encode(LoliServerConfig.get(option))
        ), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String encodedOption, String value) {
        LoliConfigOption option = LoliConfigOption.byId(encodedOption).orElse(null);
        if (option == null) {
            source.sendFailure(Component.literal("Unknown Loli option: " + encodedOption));
            return 0;
        }
        if (!LoliServerConfig.set(option, value)) {
            source.sendFailure(Component.literal(
                    "Invalid " + option.type().name().toLowerCase(Locale.ROOT)
                            + " value for " + option.id()
            ));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                option.id() + "=" + option.encode(LoliServerConfig.get(option))
        ), true);
        return 1;
    }

    private static int safeAttack(CommandSourceStack source, ServerPlayer player, String encodedEffect) {
        if (!LoliServerConfig.getBoolean(LoliConfigOption.SAFE_ATTACK_COMMAND)) {
            source.sendFailure(Component.literal("Safe Loli attacks are disabled by server configuration."));
            return 0;
        }
        SafeTntEffect effect = parseEffect(encodedEffect);
        if (effect == null) {
            source.sendFailure(Component.literal("Unknown safe Loli effect: " + encodedEffect));
            return 0;
        }
        if (!SafeTntEffectService.apply(player, effect)) {
            source.sendFailure(Component.literal("That safe effect is disabled or unavailable for the target."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Applied safe " + effect.name().toLowerCase(Locale.ROOT)
                        + " effect to " + player.getGameProfile().name()
        ), true);
        return 1;
    }

    private static int listPlayers(CommandSourceStack source, String encodedList) {
        LoliConfigOption option = playerListOption(encodedList);
        if (option == null) {
            source.sendFailure(Component.literal("Unknown Loli player list: " + encodedList));
            return 0;
        }
        String value = String.join(",", LoliLegacyExecutionPolicy.entries(option));
        source.sendSuccess(() -> Component.literal(option.id() + "=" + (value.isEmpty() ? "<empty>" : value)), false);
        return 1;
    }

    private static int addPlayer(CommandSourceStack source, String encodedList, String player) {
        LoliConfigOption option = playerListOption(encodedList);
        if (option == null || !LoliLegacyExecutionPolicy.addEntry(option, player)) {
            source.sendFailure(Component.literal(
                    "Could not add entry; use a UUID or 1-16 character player name and keep the list at 24 entries or fewer."
            ));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Added " + player + " to " + option.id()), true);
        return 1;
    }

    private static int removePlayer(CommandSourceStack source, String encodedList, String player) {
        LoliConfigOption option = playerListOption(encodedList);
        if (option == null || !LoliLegacyExecutionPolicy.removeEntry(option, player)) {
            source.sendFailure(Component.literal("That entry is not present in the selected Loli player list."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Removed " + player + " from " + option.id()), true);
        return 1;
    }

    private static LoliConfigOption playerListOption(String encoded) {
        return switch (encoded.toLowerCase(Locale.ROOT)) {
            case "reincarnation", "reincarnation_list" -> LoliConfigOption.REINCARNATION_LIST;
            case "soul_redemption", "soul_redemption_list" -> LoliConfigOption.SOUL_REDEMPTION_LIST;
            case "soul_whitelist", "soul_redemption_whitelist" -> LoliConfigOption.SOUL_REDEMPTION_WHITELIST;
            default -> null;
        };
    }

    private static SafeTntEffect parseEffect(String encoded) {
        return switch (encoded) {
            case "blue_screen", "loliPickaxeBlueScreenAttack" -> SafeTntEffect.BLUE_SCREEN;
            case "exit", "loliPickaxeExitAttack" -> SafeTntEffect.EXIT;
            case "fail_respond", "loliPickaxeFailRespondAttack" -> SafeTntEffect.FAIL_RESPOND;
            default -> null;
        };
    }

    private static CompletableFuture<Suggestions> suggestOptions(SuggestionsBuilder builder) {
        Arrays.stream(LoliConfigOption.values()).map(LoliConfigOption::id).forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestEffects(SuggestionsBuilder builder) {
        builder.suggest("blue_screen");
        builder.suggest("exit");
        builder.suggest("fail_respond");
        builder.suggest("loliPickaxeBlueScreenAttack");
        builder.suggest("loliPickaxeExitAttack");
        builder.suggest("loliPickaxeFailRespondAttack");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestPlayerLists(SuggestionsBuilder builder) {
        builder.suggest("reincarnation");
        builder.suggest("soul_redemption");
        builder.suggest("soul_whitelist");
        return builder.buildFuture();
    }

    private static boolean isAdministrator(CommandSourceStack source) {
        return source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }
}
