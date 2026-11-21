package com.ferrekkordy;

import com.ferrekkordy.screen.AddWaypointScreen;
import com.ferrekkordy.screen.WaypointListScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FerrekKordyClient implements ClientModInitializer {
    public static final String MOD_ID = "ferrekkordy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String CATEGORY = "category.ferrekkordy";
    private static KeyBinding addKey;
    private static KeyBinding listKey;
    private static KeyBinding hudKey;
    private static KeyBinding beamKey;
    private static boolean openListNextTick = false;

    @Override
    public void onInitializeClient() {
        addKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ferrekkordy.add", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY));
        listKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ferrekkordy.list", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ferrekkordy.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));
        beamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ferrekkordy.beams", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));

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

    /** Komenda zamyka czat po wykonaniu, więc okno otwieramy w następnym ticku. */
    public static void openListNextTick() {
        openListNextTick = true;
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        if (openListNextTick) {
            openListNextTick = false;
            client.setScreen(new WaypointListScreen(null));
        }

        while (addKey.wasPressed()) {
            client.setScreen(new AddWaypointScreen(null, Dims.current(client),
                    client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ()));
        }
        while (listKey.wasPressed()) {
            client.setScreen(new WaypointListScreen(null));
        }
        while (hudKey.wasPressed()) {
            CoordsHud.enabled = !CoordsHud.enabled;
        }
        while (beamKey.wasPressed()) {
            BeamRenderer.enabled = !BeamRenderer.enabled;
            client.inGameHud.setOverlayMessage(net.minecraft.text.Text.literal(
                    "Słupy waypointów: " + (BeamRenderer.enabled ? "włączone" : "wyłączone")), false);
        }
    }
}
