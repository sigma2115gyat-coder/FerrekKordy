package com.ferrekkordy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** HUD w lewym górnym rogu: koordynaty w obu wymiarach + najbliższe waypointy. */
public final class CoordsHud {
    public static boolean enabled = true;
    private static final int MAX_WAYPOINTS = 5;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int OVERWORLD_COLOR = 0xFF55FF55;
    private static final int NETHER_COLOR = 0xFFFF6644;
    private static final int END_COLOR = 0xFFDD99FF;

    private record Line(String text, int color, int dotColor) {}

    private CoordsHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (!enabled || player == null || mc.world == null) return;
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        String dim = Dims.current(mc);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        int by = player.getBlockY();

        List<Line> lines = new ArrayList<>();

        if (Dims.OVERWORLD.equals(dim)) {
            lines.add(new Line("▶ Overworld  X: " + player.getBlockX() + "  Y: " + by + "  Z: " + player.getBlockZ(), OVERWORLD_COLOR, 0));
            lines.add(new Line("   Nether      X: " + Dims.toNether(px) + "  Y: " + Dims.clampNetherY(by) + "  Z: " + Dims.toNether(pz), GRAY, 0));
        } else if (Dims.NETHER.equals(dim)) {
            lines.add(new Line("▶ Nether      X: " + player.getBlockX() + "  Y: " + by + "  Z: " + player.getBlockZ(), NETHER_COLOR, 0));
            lines.add(new Line("   Overworld  X: " + Dims.toOverworld(px) + "  Y: " + by + "  Z: " + Dims.toOverworld(pz), GRAY, 0));
        } else {
            lines.add(new Line("▶ " + Dims.pretty(dim) + "  X: " + player.getBlockX() + "  Y: " + by + "  Z: " + player.getBlockZ(), END_COLOR, 0));
        }
        lines.add(new Line("Kierunek: " + facing(player), WHITE, 0));

        // Najbliższe waypointy w aktualnym wymiarze (także te przeliczone z drugiego wymiaru).
        record Near(Waypoint wp, int[] pos, double dist) {}
        List<Near> near = new ArrayList<>();
        for (Waypoint wp : WaypointManager.all()) {
            if (!wp.visible) continue;
            int[] pos = wp.posIn(dim);
            if (pos == null) continue;
            double dx = pos[0] + 0.5 - px, dy = pos[1] - py, dz = pos[2] + 0.5 - pz;
            near.add(new Near(wp, pos, Math.sqrt(dx * dx + dy * dy + dz * dz)));
        }
        near.sort(Comparator.comparingDouble(Near::dist));

        if (!near.isEmpty()) {
            lines.add(new Line("Waypointy:", GRAY, 0));
            for (int i = 0; i < Math.min(MAX_WAYPOINTS, near.size()); i++) {
                Near n = near.get(i);
                String arrow = arrow(player, n.pos()[0] + 0.5 - px, n.pos()[2] + 0.5 - pz);
                lines.add(new Line(n.wp().name + "  " + Waypoint.fmt(n.pos()) + "  " + Math.round(n.dist()) + "m " + arrow,
                        WHITE, n.wp().color));
            }
        }

        draw(ctx, mc.textRenderer, lines);
    }

    private static void draw(DrawContext ctx, TextRenderer tr, List<Line> lines) {
        int x = 4, y = 4, pad = 3, lineH = 10, dotW = 8;
        int width = 0;
        for (Line l : lines) {
            width = Math.max(width, tr.getWidth(l.text()) + (l.dotColor() != 0 ? dotW : 0));
        }
        ctx.fill(x - pad + 2, y - pad + 2, x + width + pad, y + lines.size() * lineH + pad - 1, 0x90000000);

        for (Line l : lines) {
            int tx = x + 2;
            if (l.dotColor() != 0) {
                ctx.fill(tx, y + 1, tx + 5, y + 6, l.dotColor());
                tx += dotW;
            }
            ctx.drawTextWithShadow(tr, l.text(), tx, y, l.color());
            y += lineH;
        }
    }

    private static String facing(ClientPlayerEntity player) {
        return switch (player.getHorizontalFacing()) {
            case NORTH -> "Północ (-Z)";
            case SOUTH -> "Południe (+Z)";
            case EAST -> "Wschód (+X)";
            case WEST -> "Zachód (-X)";
            default -> "?";
        };
    }

    /** Strzałka pokazująca, w którą stronę obrócić się do celu. */
    private static String arrow(ClientPlayerEntity player, double dx, double dz) {
        if (dx * dx + dz * dz < 1.0) return "●";
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        int idx = Math.floorMod((int) Math.round(rel / 45.0), 8);
        return new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"}[idx];
    }
}
