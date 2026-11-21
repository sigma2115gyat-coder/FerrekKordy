package com.ferrekkordy.screen;

import com.ferrekkordy.Dims;
import com.ferrekkordy.Waypoint;
import com.ferrekkordy.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/** Okienko do nazwania nowego waypointu. Pokazuje od razu koordy w obu wymiarach. */
public class AddWaypointScreen extends Screen {
    private final Screen parent;
    private final String dimension;
    private final int x, y, z;
    private TextFieldWidget nameField;

    public AddWaypointScreen(Screen parent, String dimension, int x, int y, int z) {
        super(Text.literal("Nowy waypoint"));
        this.parent = parent;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        String oldText = nameField != null ? nameField.getText() : WaypointManager.nextDefaultName();
        nameField = new TextFieldWidget(this.textRenderer, cx - 100, cy - 10, 200, 20, Text.literal("Nazwa"));
        nameField.setMaxLength(32);
        nameField.setText(oldText);
        this.addDrawableChild(nameField);
        this.setInitialFocus(nameField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> save())
                .dimensions(cx - 100, cy + 18, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Anuluj"), b -> close())
                .dimensions(cx + 2, cy + 18, 98, 20).build());
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = WaypointManager.nextDefaultName();
        Waypoint wp = WaypointManager.add(name, dimension, x, y, z);
        if (this.client != null) {
            this.client.inGameHud.getChatHud().addMessage(Text.literal("Dodano waypoint ").formatted(Formatting.GREEN)
                    .append(Text.literal(wp.name).formatted(Formatting.WHITE))
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
        int cy = this.height / 2;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, cy - 62, 0xFFFFFFFF);

        Waypoint preview = new Waypoint("", dimension, x, y, z, 0);
        if (Dims.isLinked(dimension)) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Overworld: " + Waypoint.fmt(preview.posIn(Dims.OVERWORLD))), cx, cy - 46, 0xFF55FF55);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Nether: " + Waypoint.fmt(preview.posIn(Dims.NETHER))), cx, cy - 34, 0xFFFF6644);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(Dims.pretty(dimension) + ": " + Waypoint.fmt(preview.posIn(dimension))), cx, cy - 40, 0xFFDD99FF);
        }
    }
}
