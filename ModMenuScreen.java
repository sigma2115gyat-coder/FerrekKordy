package com.ferrekkordy;

import com.ferrekkordy.screen.WaypointListScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Główne menu moda KordyXYZ (klawisz M albo /wp menu). */
public class ModMenuScreen extends Screen {
    private final Screen parent;

    public ModMenuScreen(Screen parent) {
        super(Text.literal("KordyXYZ"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig c = ModConfig.get();
        int cx = this.width / 2;
        int top = Math.max(30, this.height / 2 - 100);
        int w = 150, h = 20, gap = 4;
        int lx = cx - w - gap / 2;
        int rx = cx + gap / 2;

        // Lewa kolumna: HUD
        toggle(lx, top, w, "Koordynaty", () -> c.hud, v -> c.hud = v);
        toggle(lx, top + 24, w, "Drugi wymiar", () -> c.otherDimension, v -> c.otherDimension = v);
        toggle(lx, top + 48, w, "Kierunek", () -> c.facing, v -> c.facing = v);
        toggle(lx, top + 72, w, "Tło napisów", () -> c.hudBackground, v -> c.hudBackground = v);

        // Prawa kolumna: waypointy
        toggle(rx, top, w, "Słupy", () -> c.beams, v -> c.beams = v);
        toggle(rx, top + 24, w, "Napisy nad słupami", () -> c.beamLabels, v -> c.beamLabels = v);
        toggle(rx, top + 48, w, "Waypointy w rogu", () -> c.hudWaypoints, v -> c.hudWaypoints = v);

        this.addDrawableChild(ButtonWidget.builder(positionText(), b -> {
            c.hudPosition = (c.hudPosition + 1) % ModConfig.POSITIONS.length;
            ModConfig.save();
            b.setMessage(positionText());
        }).dimensions(rx, top + 72, w, h).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("★ Waypointy...").formatted(Formatting.GOLD), b -> {
            if (this.client != null) this.client.setScreen(new WaypointListScreen(this));
        }).dimensions(lx, top + 112, w * 2 + gap, h).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(cx - 100, top + 150, 200, h).build());
    }

    private void toggle(int x, int y, int w, String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        this.addDrawableChild(ButtonWidget.builder(toggleText(label, get.get()), b -> {
            set.accept(!get.get());
            ModConfig.save();
            b.setMessage(toggleText(label, get.get()));
        }).dimensions(x, y, w, 20).build());
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": ")
                .append(on ? Text.literal("WŁ").formatted(Formatting.GREEN) : Text.literal("WYŁ").formatted(Formatting.RED));
    }

    private static Text positionText() {
        return Text.literal("Pozycja: " + ModConfig.POSITIONS[ModConfig.get().hudPosition]);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int top = Math.max(30, this.height / 2 - 100);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("✦ KordyXYZ ✦").formatted(Formatting.GOLD, Formatting.BOLD), cx, top - 22, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Ustawienia zapisują się same"), cx, top + 98, 0xFF888888);
    }
}
