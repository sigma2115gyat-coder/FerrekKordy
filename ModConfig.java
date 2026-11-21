package com.ferrekkordy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia moda (menu FerrekKordy). Zapisywane w config/ferrekkordy/ustawienia.json. */
public class ModConfig {
    public static final String[] POSITIONS = {"Lewy górny róg", "Prawy górny róg", "Lewy dolny róg", "Prawy dolny róg"};

    /** Kolory do wyboru dla waypointów. */
    public static final int[] PALETTE = {
            0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55, 0xFF00AA00, 0xFF55FFFF, 0xFF00AAAA, 0xFF55AAFF,
            0xFF5555FF, 0xFFAA55FF, 0xFFFF55FF, 0xFFAA0000, 0xFFFFFFFF, 0xFFAAAAAA, 0xFF555555, 0xFF8B5A2B
    };

    /** Ikony do wyboru dla waypointów. */
    public static final String[] ICONS = {
            "●", "★", "♥", "⌂", "⚑", "☠", "⚔", "⛏", "✿", "☀",
            "◆", "▲", "⚓", "☢", "✚", "♪", "☁", "✉", "☺", "⚡"
    };

    public boolean hud = true;
    public boolean otherDimension = true;
    public boolean facing = true;
    public boolean hudWaypoints = false;
    public boolean hudBackground = true;
    public int hudPosition = 0;
    public boolean beams = true;
    public boolean beamLabels = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance = new ModConfig();

    public static ModConfig get() {
        return instance;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("ferrekkordy").resolve("ustawienia.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(r, ModConfig.class);
            if (loaded != null) instance = loaded;
        } catch (Exception e) {
            FerrekKordyClient.LOGGER.error("Nie udało się wczytać ustawień", e);
        }
        instance.hudPosition = Math.floorMod(instance.hudPosition, POSITIONS.length);
    }

    public static void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            FerrekKordyClient.LOGGER.error("Nie udało się zapisać ustawień", e);
        }
    }
}
