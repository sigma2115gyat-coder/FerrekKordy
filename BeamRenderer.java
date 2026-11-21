package com.ferrekkordy;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Świecące słupy (jak z beacona) w miejscu waypointów + napis z nazwą i odległością.
 * Waypointy dalej niż zasięg renderowania są rysowane na granicy zasięgu,
 * w dobrą stronę, więc słup zawsze widać na horyzoncie.
 */
public final class BeamRenderer {
    private static final Identifier BEAM_TEXTURE = Identifier.ofVanilla("textures/entity/beacon_beam.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private BeamRenderer() {}

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (!ModConfig.get().beams || world == null || mc.player == null) return;
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        String dim = Dims.current(mc);
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        double maxDist = Math.max(32, mc.options.getClampedViewDistance() * 16 * 0.85);

        int bottom = world.getBottomY();
        int top = bottom + world.getHeight();
        float scroll = (System.currentTimeMillis() % 4000L) / 4000f;

        for (Waypoint wp : WaypointManager.all()) {
            if (!wp.visible) continue;
            int[] pos = wp.posIn(dim);
            if (pos == null) continue;

            double dx = pos[0] + 0.5 - cam.x;
            double dz = pos[2] + 0.5 - cam.z;
            double dy = pos[1] + 0.5 - cam.y;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            double realDist = Math.sqrt(horiz * horiz + dy * dy);

            // Za daleko? Przesuń słup na granicę zasięgu, w tym samym kierunku.
            double bx = dx, bz = dz;
            if (horiz > maxDist) {
                bx = dx / horiz * maxDist;
                bz = dz / horiz * maxDist;
            }
            double shownHoriz = Math.min(horiz, maxDist);

            // Grubość słupa rośnie z odległością, żeby był widoczny z daleka.
            float w = (float) Math.max(0.2, shownHoriz * 0.004);
            int rgb = wp.color & 0xFFFFFF;

            if (horiz > 2.5) {
                float y1 = (float) (bottom - cam.y);
                float y2 = (float) (top - cam.y);
                drawColumn(matrices, consumers.getBuffer(RenderLayer.getBeaconBeam(BEAM_TEXTURE, false)),
                        (float) bx, (float) bz, w, y1, y2, 0xFF000000 | rgb, scroll);
                drawColumn(matrices, consumers.getBuffer(RenderLayer.getBeaconBeam(BEAM_TEXTURE, true)),
                        (float) bx, (float) bz, w * 1.8f, y1, y2, 0x40000000 | rgb, scroll * 0.5f);
            }

            if (ModConfig.get().beamLabels) {
                double labelY = horiz > maxDist ? Math.max(Math.min(dy, 40), -10) : dy + 2.0;
                drawLabel(mc.textRenderer, matrices, consumers, camera, bx, labelY, bz,
                        wp.icon() + " " + wp.name + "  " + Math.round(realDist) + "m", 0xFF000000 | rgb, shownHoriz);
            }
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    private static void drawColumn(MatrixStack matrices, VertexConsumer vc, float cx, float cz, float w,
                                   float y1, float y2, int color, float scroll) {
        MatrixStack.Entry e = matrices.peek();
        float v1 = -scroll;
        float v2 = v1 + (y2 - y1) / (w * 8f);
        float x0 = cx - w, x1 = cx + w, z0 = cz - w, z1 = cz + w;
        side(vc, e, x0, z0, x1, z0, y1, y2, color, v1, v2);
        side(vc, e, x1, z0, x1, z1, y1, y2, color, v1, v2);
        side(vc, e, x1, z1, x0, z1, y1, y2, color, v1, v2);
        side(vc, e, x0, z1, x0, z0, y1, y2, color, v1, v2);
    }

    /** Jedna ściana słupa, rysowana z obu stron (żeby nie znikała przez culling). */
    private static void side(VertexConsumer vc, MatrixStack.Entry e, float xa, float za, float xb, float zb,
                             float y1, float y2, int color, float v1, float v2) {
        vertex(vc, e, xa, y2, za, color, 0f, v1);
        vertex(vc, e, xa, y1, za, color, 0f, v2);
        vertex(vc, e, xb, y1, zb, color, 1f, v2);
        vertex(vc, e, xb, y2, zb, color, 1f, v1);

        vertex(vc, e, xb, y2, zb, color, 1f, v1);
        vertex(vc, e, xb, y1, zb, color, 1f, v2);
        vertex(vc, e, xa, y1, za, color, 0f, v2);
        vertex(vc, e, xa, y2, za, color, 0f, v1);
    }

    private static void vertex(VertexConsumer vc, MatrixStack.Entry e, float x, float y, float z,
                               int color, float u, float v) {
        vc.vertex(e, x, y, z)
                .color(color)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(e, 0f, 1f, 0f);
    }

    /** Napis zawsze zwrócony do gracza i widoczny przez ściany. */
    private static void drawLabel(TextRenderer tr, MatrixStack matrices, VertexConsumerProvider consumers,
                                  Camera camera, double x, double y, double z, String text, int color, double dist) {
        float s = (float) Math.max(0.025, dist * 0.0035);
        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(camera.getRotation());
        matrices.scale(s, -s, s);
        Matrix4f m = matrices.peek().getPositionMatrix();
        float tx = -tr.getWidth(text) / 2f;
        tr.draw(text, tx, 0f, color, false, m, consumers, TextRenderer.TextLayerType.SEE_THROUGH, 0x80000000, FULL_BRIGHT);
        matrices.pop();
    }
}
