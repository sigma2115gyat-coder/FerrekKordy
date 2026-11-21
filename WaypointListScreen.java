package com.ferrekkordy.screen;

import com.ferrekkordy.Dims;
import com.ferrekkordy.Waypoint;
import com.ferrekkordy.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Lista waypointów: koordy w Overworldzie i Netherze, edycja (kolor, ikona, nazwa), ukrywanie i usuwanie. */
public class WaypointListScreen extends Screen {
    private static final int TOP = 34;
    private static final int ROW_H = 26;
    private final Screen parent;
    private int page = 0;

    public WaypointListScreen(Screen parent) {
        super(Text.literal("Waypointy"));
        this.parent = parent;
    }

    private int panelWidth() {
        return Math.min(this.width - 20, 440);
    }

    private int left() {
        return (this.width - panelWidth()) / 2;
    }

    private int rowsPerPage() {
        return Math.max(1, (this.height - TOP - 40) / ROW_H);
    }

    private int pageCount() {
        return Math.max(1, (WaypointManager.all().size() + rowsPerPage() - 1) / rowsPerPage());
    }

    @Override
    protected void init() {
        page = Math.max(0, Math.min(page, pageCount() - 1));
        List<Waypoint> list = WaypointManager.all();
        int per = rowsPerPage();
        int right = left() + panelWidth();

        for (int i = 0; i < per; i++) {
            int index = page * per + i;
            if (index >= list.size()) break;
            Waypoint wp = list.get(index);
            int rowY = TOP + i * ROW_H;

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Edytuj"), b -> {
                if (this.client != null) this.client.setScreen(new AddWaypointScreen(this, wp));
            }).dimensions(right - 154, rowY + 2, 50, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal(wp.visible ? "Ukryj" : "Pokaż"), b -> {
                wp.visible = !wp.visible;
                WaypointManager.save();
                this.clearAndInit();
            }).dimensions(right - 102, rowY + 2, 52, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Usuń").formatted(Formatting.RED), b -> {
                WaypointManager.remove(wp);
                this.clearAndInit();
            }).dimensions(right - 48, rowY + 2, 48, 20).build());
        }

        int by = this.height - 28;
        int cx = this.width / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            page--;
            this.clearAndInit();
        }).dimensions(cx - 154, by, 20, 20).build()).active = page > 0;
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            page++;
            this.clearAndInit();
        }).dimensions(cx + 134, by, 20, 20).build()).active = page < pageCount() - 1;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj tutaj"), b -> {
            if (this.client != null && this.client.player != null) {
                var p = this.client.player;
                this.client.setScreen(new AddWaypointScreen(this, Dims.current(this.client),
                        p.getBlockX(), p.getBlockY(), p.getBlockZ()));
            }
        }).dimensions(cx - 130, by, 128, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal(parent != null ? "Wróć" : "Zamknij"), b -> close())
                .dimensions(cx + 2, by, 128, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Waypointy  (" + (page + 1) + "/" + pageCount() + ")"), this.width / 2, 14, 0xFFFFFFFF);

        List<Waypoint> list = WaypointManager.all();
        if (list.isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Brak waypointów – kliknij „Dodaj tutaj” albo wciśnij B w grze"),
                    this.width / 2, this.height / 2 - 10, 0xFFAAAAAA);
            return;
        }

        int per = rowsPerPage();
        int left = left();
        for (int i = 0; i < per; i++) {
            int index = page * per + i;
            if (index >= list.size()) break;
            Waypoint wp = list.get(index);
            int rowY = TOP + i * ROW_H;

            context.fill(left, rowY, left + panelWidth(), rowY + ROW_H - 2, 0x60000000);

            int iconColor = wp.visible ? wp.color : 0xFF555555;
            int nameColor = wp.visible ? 0xFFFFFFFF : 0xFF777777;
            context.drawTextWithShadow(this.textRenderer, wp.icon(), left + 5, rowY + 4, iconColor);
            int nx = left + 5 + this.textRenderer.getWidth(wp.icon() + " ");
            context.drawTextWithShadow(this.textRenderer, wp.name, nx, rowY + 4, nameColor);
            context.drawTextWithShadow(this.textRenderer, "  (" + Dims.pretty(wp.dimension) + ")",
                    nx + this.textRenderer.getWidth(wp.name), rowY + 4, 0xFF777777);

            String coords = wp.isLinked()
                    ? "OW: " + Waypoint.fmt(wp.posIn(Dims.OVERWORLD)) + "   Nether: " + Waypoint.fmt(wp.posIn(Dims.NETHER))
                    : Dims.pretty(wp.dimension) + ": " + Waypoint.fmt(wp.posIn(wp.dimension));
            context.drawTextWithShadow(this.textRenderer, coords, left + 5, rowY + 14, 0xFFAAAAAA);
        }
    }
}
