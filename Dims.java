package com.ferrekkordy;

import net.minecraft.client.MinecraftClient;

/** Pomocnicze rzeczy do wymiarów i przeliczania koordynatów (1 blok w Netherze = 8 bloków w Overworldzie). */
public final class Dims {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";

    private Dims() {}

    public static String current(MinecraftClient client) {
        if (client.world == null) return OVERWORLD;
        return client.world.getRegistryKey().getValue().toString();
    }

    /** Czy wymiar ma odpowiednik (Overworld <-> Nether). */
    public static boolean isLinked(String dim) {
        return OVERWORLD.equals(dim) || NETHER.equals(dim);
    }

    public static String pretty(String dim) {
        return switch (dim) {
            case OVERWORLD -> "Overworld";
            case NETHER -> "Nether";
            case END -> "End";
            default -> dim;
        };
    }

    /** Overworld -> Nether (zaokrąglanie w dół, tak jak w grze). */
    public static int toNether(double overworldCoord) {
        return (int) Math.floor(overworldCoord / 8.0);
    }

    /** Nether -> Overworld. */
    public static int toOverworld(double netherCoord) {
        return (int) Math.floor(netherCoord * 8.0);
    }

    /** Wysokość w Netherze ograniczona do sensownego zakresu (pod dachem z bedrocka). */
    public static int clampNetherY(int y) {
        return Math.max(1, Math.min(125, y));
    }
}
