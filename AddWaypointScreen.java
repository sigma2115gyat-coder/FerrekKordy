package com.ferrekkordy.screen;

import com.ferrekkordy.Dims;
import com.ferrekkordy.ModConfig;
import com.ferrekkordy.Waypoint;
import com.ferrekkordy.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Edytor waypointu: nazwa, kolor, ikona.
 * Działa dla nowego waypointu (dodawanie) i dla istniejącego (edycja).
 */
public class AddWaypointScreen extends Screen {
    private static final int SQ = 20;
    private static final int GAP = 2;

    private final Screen parent;
    private final Waypoint editing;
    private final String dimension;
    private final int x, y, z;

    private TextFieldWidget nameField;
    private String nameText;
    private int color;
    private String icon;

    private final List<ButtonWidget> colorButtons = new ArrayList<>();
    private final List<ButtonWidget> iconButtons = new ArrayList<>();

    /** Nowy waypoint w podanym miejscu. */
    public AddWaypointScreen(Screen parent, String dimension, int x, int y, int z) {
        super(Text.literal("Nowy waypoint"));
        this.parent = parent;
        this.editing = null;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.nameText = WaypointManager.nextDefaultName();
        this.color = ModConfig.PALETTE[WaypointManager.all().size() % 11];
        this.icon = ModConfig.ICONS[0];
    }

    /** Edycja istniejącego waypointu. */
    public AddWaypointScreen(Screen parent, Waypoint wp) {
        super(Text.literal("Edytuj waypoint"));
        this.parent = parent;
        this.editing = wp;
        this.dimension = wp.dimension;
        this.x = wp.x;
        this.y = wp.y;
        this.z = wp.z;
        this.nameText = wp.name;
        this.color = wp.color;
        this.icon = wp.icon();
    }

    private int left() {
        return this.width / 2 - 110;
    }

    private int topY() {
        return Math.max(4, this.height / 2 - 118);
    }

    @Override
    protected void init() {
        colorButtons.clear();
        iconButtons.clear();
        int left = left();
        int top = topY();

        if (nameField != null) nameText = nameField.getText();
        nameField = new TextFieldWidget(this.textRenderer, left, top + 40, 220, 20, Text.literal("Nazwa"));
        nameField.setMaxLength(32);
        nameField.setText(nameText);
        this.addDrawableChild(nameField);
        this.setInitialFocus(nameField);

        // Kolory: 2 rzędy po 8
        int colorsY = top + 76;
        for (int i = 0; i < ModConfig.PALETTE.length; i++) {
            final int c = ModConfig.PALETTE[i];
            int bx = left + (i % 8) * (SQ + GAP);
            int by = colorsY + (i / 8) * (SQ + GAP);
            colorButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
                color = c;
                refreshIconButtons();
            }).dimensions(bx, by, SQ, SQ).build()));
        }

        // Ikony: 2 rzędy po 10
        int iconsY = top + 136;
        for (int i = 0; i < ModConfig.ICONS.length; i++) {
            final String ic = ModConfig.ICONS[i];
            int bx = left + (i % 10) * (SQ + GAP);
            int by = iconsY + (i / 10) * (SQ + GAP);
            iconButtons.add(this.addDrawableChild(ButtonWidget.builder(Text.literal(ic), b -> icon = ic)
                    .dimensions(bx, by, SQ, SQ).build()));
        }
        refreshIconButtons();

        int buttonsY = top + 190;
        if (editing == null) {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                    .dimensions(left, buttonsY, 108, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                    .dimensions(left + 112, buttonsY, 108, 20).build());
        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                    .dimensions(left, buttonsY, 72, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Usuń").formatted(Formatting.RED), b -> {
                WaypointManager.remove(editing);
                close();
            }).dimensions(left + 74, buttonsY, 72, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                    .dimensions(left + 148, buttonsY, 72, 20).build());
        }
    }

    /** Ikony na przyciskach w aktualnie wybranym kolorze. */
    private void refreshIconButtons() {
        for (int i = 0; i < iconButtons.size(); i++) {
            iconButtons.get(i).setMessage(Text.literal(ModConfig.ICONS[i]).withColor(color & 0xFFFFFF));
        }
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = WaypointManager.nextDefaultName();

        Waypoint wp;
        if (editing == null) {
            wp = WaypointManager.add(name, dimension, x, y, z);
        } else {
            wp = editing;
            WaypointManager.rename(wp, name);
        }
        wp.color = color;
        wp.icon = icon;
        WaypointManager.save();

        if (editing == null && this.client != null) {
            this.client.inGameHud.getChatHud().addMessage(Text.literal("Dodano waypoint ").formatted(Formatting.GREEN)
                    .append(Text.literal(wp.icon() + " " + wp.name).withColor(wp.color & 0xFFFFFF))
                    .append(Text.literal("  " + wp.describe()).formatted(Formatting.GRAY)));
        }
        close();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int cx = this.width / 2;
        int left = left();
        int top = topY();

        // Podgląd: ikona + nazwa w wybranym kolorze
        String preview = icon + " " + nameField.getText();
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, top, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(preview), cx, top + 12, color);

        Waypoint tmp = new Waypoint("", dimension, x, y, z, 0);
        String coords = Dims.isLinked(dimension)
                ? "Overworld: " + Waypoint.fmt(tmp.posIn(Dims.OVERWORLD)) + "   Nether: " + Waypoint.fmt(tmp.posIn(Dims.NETHER))
                : Dims.pretty(dimension) + ": " + Waypoint.fmt(tmp.posIn(dimension));
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(coords), cx, top + 24, 0xFFAAAAAA);

        context.drawTextWithShadow(this.textRenderer, "Kolor:", left, top + 66, 0xFFFFFFFF);
        context.drawTextWithShadow(this.textRenderer, "Ikona:", left, top + 126, 0xFFFFFFFF);

        // Kolorowe kwadraty na przyciskach + ramka wokół wybranego
        for (int i = 0; i < colorButtons.size(); i++) {
            ButtonWidget b = colorButtons.get(i);
            int c = ModConfig.PALETTE[i];
            context.fill(b.getX() + 3, b.getY() + 3, b.getX() + SQ - 3, b.getY() + SQ - 3, c);
            if (c == color) outline(context, b.getX(), b.getY(), SQ, SQ, 0xFFFFFFFF);
        }
        for (int i = 0; i < iconButtons.size(); i++) {
            if (ModConfig.ICONS[i].equals(icon)) {
                ButtonWidget b = iconButtons.get(i);
                outline(context, b.getX(), b.getY(), SQ, SQ, 0xFFFFFFFF);
            }
        }
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x - 1, y - 1, x + w + 1, y, c);
        ctx.fill(x - 1, y + h, x + w + 1, y + h + 1, c);
        ctx.fill(x - 1, y, x, y + h, c);
        ctx.fill(x + w, y, x + w + 1, y + h, c);
    }
}
