package com.ferrekkordy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.WorldSavePath;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Trzyma waypointy dla aktualnego świata/serwera i zapisuje je w config/ferrekkordy/. */
public final class WaypointManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>() {}.getType();

    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();
    private static Path file;

    private WaypointManager() {}

    public static void load(MinecraftClient client) {
        WAYPOINTS.clear();
        file = FabricLoader.getInstance().getConfigDir()
                .resolve("ferrekkordy")
                .resolve(worldKey(client) + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                List<Waypoint> loaded = GSON.fromJson(reader, LIST_TYPE);
                if (loaded != null) {
                    for (Waypoint wp : loaded) {
                        if (wp != null && wp.name != null && wp.dimension != null) WAYPOINTS.add(wp);
                    }
                }
            } catch (Exception e) {
                FerrekKordyClient.LOGGER.error("Nie udało się wczytać waypointów z {}", file, e);
            }
        }
    }

    public static void unload() {
        WAYPOINTS.clear();
        file = null;
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(WAYPOINTS, LIST_TYPE, writer);
            }
        } catch (Exception e) {
            FerrekKordyClient.LOGGER.error("Nie udało się zapisać waypointów do {}", file, e);
        }
    }

    public static List<Waypoint> all() {
        return Collections.unmodifiableList(WAYPOINTS);
    }

    public static Waypoint add(String name, String dimension, int x, int y, int z) {
        Waypoint wp = new Waypoint(uniqueName(name, null), dimension, x, y, z, ModConfig.PALETTE[WAYPOINTS.size() % 11]);
        WAYPOINTS.add(wp);
        save();
        return wp;
    }

    public static boolean remove(Waypoint wp) {
        boolean removed = WAYPOINTS.remove(wp);
        if (removed) save();
        return removed;
    }

    /** Zmiana nazwy z pilnowaniem, żeby dwa waypointy nie miały tej samej nazwy. */
    public static void rename(Waypoint wp, String newName) {
        wp.name = uniqueName(newName, wp);
    }

    public static Waypoint find(String name) {
        for (Waypoint wp : WAYPOINTS) {
            if (wp.name.equalsIgnoreCase(name)) return wp;
        }
        return null;
    }

    public static String nextDefaultName() {
        return uniqueName("Punkt " + (WAYPOINTS.size() + 1), null);
    }

    private static String uniqueName(String base, Waypoint except) {
        String name = base.isBlank() ? "Punkt" : base.trim();
        if (!taken(name, except)) return name;
        int i = 2;
        while (taken(name + " (" + i + ")", except)) i++;
        return name + " (" + i + ")";
    }

    private static boolean taken(String name, Waypoint except) {
        Waypoint other = find(name);
        return other != null && other != except;
    }

    /** Osobny plik dla każdego świata w singlu i każdego serwera. */
    private static String worldKey(MinecraftClient client) {
        String key;
        if (client.getServer() != null) {
            String folder;
            try {
                folder = client.getServer().getSavePath(WorldSavePath.ROOT).normalize().getFileName().toString();
            } catch (Exception e) {
                folder = client.getServer().getSaveProperties().getLevelName();
            }
            key = "sp_" + folder;
        } else {
            ServerInfo info = client.getCurrentServerEntry();
            key = info != null ? "mp_" + info.address : "mp_nieznany";
        }
        return key.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
