package com.ferrekkordy;

import com.ferrekkordy.screen.AddWaypointScreen;
import com.ferrekkordy.screen.WaypointListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FerrekKordyClient implements ClientModInitializer {
    public static final String MOD_ID = "ferrekkordy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String CATEGORY = "category.ferrekkordy";
    private static KeyBinding menuKey;
    private static KeyBinding addKey;
    private static KeyBinding listKey;
    private static KeyBinding hudKey;
    private static KeyBinding beamKey;
    private static Screen pendingScreen = null;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        menuKey = register("key.ferrekkordy.menu", GLFW.GLFW_KEY_M);
        addKey = register("key.ferrekkordy.add", GLFW.GLFW_KEY_B);
        listKey = register("key.ferrekkordy.list", GLFW.GLFW_KEY_N);
        hudKey = register("key.ferrekkordy.hud", GLFW.GLFW_KEY_H);
        beamKey = register("key.ferrekkordy.beams", GLFW.GLFW_KEY_K);

        ClientTickEvents.END_CLIENT_TICK.register(FerrekKordyClient::onTick);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "coords"), CoordsHud::render);

        WorldRenderEvents.AFTER_ENTITIES.register(BeamRenderer::render);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WaypointManager.load(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WaypointManager.unload());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                WaypointCommands.register(dispatcher));

        LOGGER.info("FerrekKordy załadowany");
    }

    private static KeyBinding register(String key, int code) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(key, InputUtil.Type.KEYSYM, code, CATEGORY));
    }

    /** Komenda zamyka czat po wykonaniu, więc okno otwieramy w następnym ticku. */
    public static void openNextTick(Screen screen) {
        pendingScreen = screen;
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        if (pendingScreen != null) {
            client.setScreen(pendingScreen);
            pendingScreen = null;
        }

        ModConfig cfg = ModConfig.get();
        while (menuKey.wasPressed()) {
            client.setScreen(new ModMenuScreen(null));
        }
        while (addKey.wasPressed()) {
            client.setScreen(new AddWaypointScreen(null, Dims.current(client),
                    client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ()));
        }
        while (listKey.wasPressed()) {
            client.setScreen(new WaypointListScreen(null));
        }
        while (hudKey.wasPressed()) {
            cfg.hud = !cfg.hud;
            ModConfig.save();
        }
        while (beamKey.wasPressed()) {
            cfg.beams = !cfg.beams;
            ModConfig.save();
            client.inGameHud.setOverlayMessage(Text.literal(
                    "Słupy waypointów: " + (cfg.beams ? "włączone" : "wyłączone")), false);
        }
    }
}
