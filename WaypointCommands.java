package com.ferrekkordy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * /wp add <nazwa>            – waypoint w miejscu, gdzie stoisz
 * /wp at <x> <y> <z> <nazwa> – waypoint na podanych koordynatach (w wymiarze, w którym jesteś)
 * /wp del <nazwa>            – usuń
 * /wp list                   – lista w czacie
 * /wp gui                    – okno z listą
 * /wp menu                   – menu moda
 * /wp calc <x> <z>           – szybki kalkulator Overworld <-> Nether
 * /wp hud                    – pokaż/ukryj HUD
 * /wp beams                  – pokaż/ukryj słupy w świecie
 */
public final class WaypointCommands {
    private WaypointCommands() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("wp")
                .executes(WaypointCommands::help)
                .then(literal("add")
                        .then(argument("nazwa", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    FabricClientCommandSource src = ctx.getSource();
                                    var p = src.getPlayer();
                                    return add(src, StringArgumentType.getString(ctx, "nazwa"),
                                            p.getBlockX(), p.getBlockY(), p.getBlockZ());
                                })))
                .then(literal("at")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("y", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .then(argument("nazwa", StringArgumentType.greedyString())
                                                        .executes(ctx -> add(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "nazwa"),
                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                                IntegerArgumentType.getInteger(ctx, "z"))))))))
                .then(literal("del")
                        .then(argument("nazwa", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                        WaypointManager.all().stream().map(wp -> wp.name), builder))
                                .executes(WaypointCommands::delete)))
                .then(literal("list").executes(WaypointCommands::list))
                .then(literal("gui").executes(ctx -> {
                    FerrekKordyClient.openNextTick(new com.ferrekkordy.screen.WaypointListScreen(null));
                    return 1;
                }))
                .then(literal("menu").executes(ctx -> {
                    FerrekKordyClient.openNextTick(new ModMenuScreen(null));
                    return 1;
                }))
                .then(literal("hud").executes(ctx -> {
                    ModConfig.get().hud = !ModConfig.get().hud;
                    ModConfig.save();
                    ctx.getSource().sendFeedback(Text.literal("HUD koordynatów: " + (ModConfig.get().hud ? "włączony" : "wyłączony"))
                            .formatted(Formatting.YELLOW));
                    return 1;
                }))
                .then(literal("beams").executes(ctx -> {
                    ModConfig.get().beams = !ModConfig.get().beams;
                    ModConfig.save();
                    ctx.getSource().sendFeedback(Text.literal("Słupy waypointów: " + (ModConfig.get().beams ? "włączone" : "wyłączone"))
                            .formatted(Formatting.YELLOW));
                    return 1;
                }))
                .then(literal("calc")
                        .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                        .executes(WaypointCommands::calc)))));
    }

    private static int add(FabricClientCommandSource src, String name, int x, int y, int z) {
        Waypoint wp = WaypointManager.add(name, Dims.current(src.getClient()), x, y, z);
        src.sendFeedback(Text.literal("Dodano waypoint ").formatted(Formatting.GREEN)
                .append(Text.literal(wp.icon() + " " + wp.name).withColor(wp.color & 0xFFFFFF)));
        src.sendFeedback(Text.literal(wp.describe()).formatted(Formatting.GRAY));
        return 1;
    }

    private static int delete(CommandContext<FabricClientCommandSource> ctx) {
        String name = StringArgumentType.getString(ctx, "nazwa");
        Waypoint wp = WaypointManager.find(name);
        if (wp == null) {
            ctx.getSource().sendError(Text.literal("Nie ma waypointu o nazwie: " + name));
            return 0;
        }
        WaypointManager.remove(wp);
        ctx.getSource().sendFeedback(Text.literal("Usunięto waypoint " + wp.name).formatted(Formatting.YELLOW));
        return 1;
    }

    private static int list(CommandContext<FabricClientCommandSource> ctx) {
        var src = ctx.getSource();
        if (WaypointManager.all().isEmpty()) {
            src.sendFeedback(Text.literal("Brak waypointów. Użyj /wp add <nazwa> albo klawisza B.").formatted(Formatting.GRAY));
            return 1;
        }
        src.sendFeedback(Text.literal("Waypointy (" + WaypointManager.all().size() + "):").formatted(Formatting.GOLD));
        for (Waypoint wp : WaypointManager.all()) {
            src.sendFeedback(Text.literal(" • ").formatted(Formatting.GRAY)
                    .append(Text.literal(wp.icon() + " " + wp.name).withColor(wp.visible ? (wp.color & 0xFFFFFF) : 0x555555))
                    .append(Text.literal("  " + wp.describe()).formatted(Formatting.GRAY)));
        }
        return 1;
    }

    private static int calc(CommandContext<FabricClientCommandSource> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, "x");
        int z = IntegerArgumentType.getInteger(ctx, "z");
        var src = ctx.getSource();
        src.sendFeedback(Text.literal("Overworld " + x + " " + z + "  →  Nether " + Dims.toNether(x) + " " + Dims.toNether(z))
                .formatted(Formatting.GREEN));
        src.sendFeedback(Text.literal("Nether " + x + " " + z + "  →  Overworld " + Dims.toOverworld(x) + " " + Dims.toOverworld(z))
                .formatted(Formatting.RED));
        return 1;
    }

    private static int help(CommandContext<FabricClientCommandSource> ctx) {
        var src = ctx.getSource();
        src.sendFeedback(Text.literal("FerrekKordy – komendy:").formatted(Formatting.GOLD));
        src.sendFeedback(Text.literal("/wp add <nazwa> – waypoint tutaj"));
        src.sendFeedback(Text.literal("/wp at <x> <y> <z> <nazwa> – waypoint na koordach"));
        src.sendFeedback(Text.literal("/wp menu, /wp del <nazwa>, /wp list, /wp gui, /wp hud, /wp beams"));
        src.sendFeedback(Text.literal("/wp calc <x> <z> – przelicz Overworld ↔ Nether"));
        return 1;
    }
}
