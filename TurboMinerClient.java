package com.turbominer;

import com.turbominer.gui.ControlScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TurboMinerClient implements ClientModInitializer {
    public static KeyBinding menuKey;
    private static KeyBinding fastMineKey;
    private static KeyBinding speedKey;
    private static KeyBinding flyKey;
    private static KeyBinding autoMineKey;
    private static KeyBinding ghostKey;
    private static KeyBinding noclipKey, biomeKey, biomeNextKey, biomePrevKey, biomeSnowKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        menuKey = register("menu", GLFW.GLFW_KEY_KP_2);
        fastMineKey = register("fastmine", GLFW.GLFW_KEY_KP_7);
        speedKey = register("speed", GLFW.GLFW_KEY_KP_8);
        flyKey = register("fly", GLFW.GLFW_KEY_KP_6);
        autoMineKey = register("automine", GLFW.GLFW_KEY_KP_4);
        ghostKey = register("ghost", GLFW.GLFW_KEY_KP_5);
        noclipKey = register("noclip", GLFW.GLFW_KEY_KP_1);
        biomeKey = register("biome", GLFW.GLFW_KEY_KP_9);
        biomeNextKey = register("biome_next", GLFW.GLFW_KEY_KP_ADD);
        biomePrevKey = register("biome_prev", GLFW.GLFW_KEY_KP_SUBTRACT);
        biomeSnowKey = register("biome_snow", GLFW.GLFW_KEY_KP_3);

        ClientTickEvents.START_CLIENT_TICK.register(Modules::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(TurboMinerClient::onEndTick);
        HudRenderCallback.EVENT.register(HudOverlay::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Ghost.reset();
            NoClip.reset();
            BiomeCompass.reset();
        });
    }

    private static void onEndTick(MinecraftClient client) {
        while (menuKey.wasPressed()) {
            if (client.currentScreen == null && client.player != null) {
                client.setScreen(new ControlScreen());
            }
        }
        while (fastMineKey.wasPressed()) Modules.toggleFastMine();
        while (speedKey.wasPressed()) Modules.toggleSpeed();
        while (flyKey.wasPressed()) Modules.toggleFly();
        while (autoMineKey.wasPressed()) Modules.toggleAutoMine();
        while (ghostKey.wasPressed()) Modules.toggleGhost();
        while (noclipKey.wasPressed()) Modules.toggleNoClip();
        while (biomeKey.wasPressed()) BiomeCompass.toggle();
        while (biomeNextKey.wasPressed()) BiomeCompass.cycle(1);
        while (biomePrevKey.wasPressed()) BiomeCompass.cycle(-1);
        while (biomeSnowKey.wasPressed()) BiomeCompass.selectAutoSnow();

        Modules.endTick(client);
    }

    private static KeyBinding register(String id, int key) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.turbominer." + id, InputUtil.Type.KEYSYM, key, "category.turbominer"));
    }
}
