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

/** HUD z koordynatami: aktualny wymiar + przeliczenie na drugi wymiar. Co pokazuje, ustawiasz w menu moda. */
public final class CoordsHud {
    private static final int MAX_WAYPOINTS = 5;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int OVERWORLD_COLOR = 0xFF55FF55;
    private static final int NETHER_COLOR = 0xFFFF6644;
    private static final int END_COLOR = 0xFFDD99FF;

    /** icon == null -> zwykła linijka; inaczej ikona w kolorze iconColor przed tekstem. */
    private record Line(String text, int color, String icon, int iconColor) {
        Line(String text, int color) {
            this(text, color, null, 0);
        }
    }

    private CoordsHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        ModConfig cfg = ModConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (!cfg.hud || player == null || mc.world == null) return;
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        String dim = Dims.current(mc);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        int by = player.getBlockY();

        List<Line> lines = new ArrayList<>();
        if (Dims.OVERWORLD.equals(dim)) {
            lines.add(new Line("▶ Overworld  X: " + player.getBlockX() + "  Y: " + by + "  Z: " + player.getBlockZ(), OVERWORLD_COLOR));
            if (cfg.otherDimension) {
                lines.add(new Line("   Nether      X: " + Dims.toNether(px) + "  Y: " + Dims.clampNetherY(by) + "  Z: " + Dims.toNether(pz), GRAY));
            }
        } else if (Dims.NETHER.equals(dim)) {
            lines.add(new Line("▶ Nether      X: " + player.getBlockX() + "  Y: " + by + "  Z: " + player.getBlockZ(), NETHER_COLOR));
            if (cfg.otherDimension) {
                lines.add(new Line("   Overworld  X: " + Dims.toOverworld(px) + "  Y: " + by + "  Z: " + Dims.toOverworld(pz), GRAY));
            }
        } else {
            lines.add(new Line("▶ " + Dims.pretty(dim) + "  X: " + player.getBlockX() + "  Y: " + by + "  Z: " + player.getBlockZ(), END_COLOR));
        }
        if (cfg.facing) {
            lines.add(new Line("Kierunek: " + facing(player), WHITE));
        }

        if (cfg.hudWaypoints) {
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
            for (int i = 0; i < Math.min(MAX_WAYPOINTS, near.size()); i++) {
                Near n = near.get(i);
                String arrow = arrow(player, n.pos()[0] + 0.5 - px, n.pos()[2] + 0.5 - pz);
                lines.add(new Line(n.wp().name + "  " + Math.round(n.dist()) + "m " + arrow, WHITE,
                        n.wp().icon(), n.wp().color));
            }
        }

        draw(ctx, mc.textRenderer, lines, cfg);
    }

    private static void draw(DrawContext ctx, TextRenderer tr, List<Line> lines, ModConfig cfg) {
        int pad = 3, lineH = 10;
        int width = 0;
        for (Line l : lines) {
            int w = tr.getWidth(l.text()) + (l.icon() != null ? tr.getWidth(l.icon() + " ") : 0);
            width = Math.max(width, w);
        }
        int height = lines.size() * lineH;

        boolean right = cfg.hudPosition == 1 || cfg.hudPosition == 3;
        boolean bottom = cfg.hudPosition >= 2;
        int x = right ? ctx.getScaledWindowWidth() - width - 6 : 6;
        int y = bottom ? ctx.getScaledWindowHeight() - height - 6 : 6;

        if (cfg.hudBackground) {
            ctx.fill(x - pad, y - pad, x + width + pad, y + height + pad - 2, 0x90000000);
        }
        for (Line l : lines) {
            int tx = x;
            if (l.icon() != null) {
                ctx.drawTextWithShadow(tr, l.icon(), tx, y, l.iconColor());
                tx += tr.getWidth(l.icon() + " ");
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

    private static String arrow(ClientPlayerEntity player, double dx, double dz) {
        if (dx * dx + dz * dz < 1.0) return "●";
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double rel = MathHelper.wrapDegrees(targetYaw - player.getYaw());
        int idx = Math.floorMod((int) Math.round(rel / 45.0), 8);
        return new String[]{"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"}[idx];
    }
}
