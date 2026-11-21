package com.ferrekkordy;

/**
 * Waypoint zapisuje wymiar, w którym go stworzono, i koordynaty z tego wymiaru.
 * Odpowiednik w drugim wymiarze (Overworld <-> Nether) jest liczony automatycznie,
 * więc waypoint od razu "istnieje" w obu wymiarach.
 */
public class Waypoint {
    public String name;
    public String dimension;
    public int x;
    public int y;
    public int z;
    public int color;
    public boolean visible = true;

    public Waypoint() {}

    public Waypoint(String name, String dimension, int x, int y, int z, int color) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
    }

    /** Pozycja tego waypointu w podanym wymiarze albo null, jeśli nie ma tam odpowiednika. */
    public int[] posIn(String dim) {
        if (dim.equals(dimension)) return new int[]{x, y, z};
        if (Dims.OVERWORLD.equals(dimension) && Dims.NETHER.equals(dim)) {
            return new int[]{Dims.toNether(x), Dims.clampNetherY(y), Dims.toNether(z)};
        }
        if (Dims.NETHER.equals(dimension) && Dims.OVERWORLD.equals(dim)) {
            return new int[]{Dims.toOverworld(x), y, Dims.toOverworld(z)};
        }
        return null;
    }

    public boolean isLinked() {
        return Dims.isLinked(dimension);
    }

    public static String fmt(int[] p) {
        return p == null ? "-" : p[0] + " " + p[1] + " " + p[2];
    }

    /** Np. "Overworld: 800 64 -240  |  Nether: 100 64 -30" */
    public String describe() {
        if (isLinked()) {
            return "Overworld: " + fmt(posIn(Dims.OVERWORLD)) + "   Nether: " + fmt(posIn(Dims.NETHER));
        }
        return Dims.pretty(dimension) + ": " + fmt(posIn(dimension));
    }
}
