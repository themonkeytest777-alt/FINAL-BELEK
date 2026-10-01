package com.turbominer;

import com.turbominer.gui.ControlScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Petit affichage en haut a gauche des modules actifs. */
public final class HudOverlay {
    private HudOverlay() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.options.hudHidden || c.currentScreen instanceof ControlScreen) {
            return;
        }
        BiomeCompass.render(ctx, c);
        if (!ModConfig.showHud) return;
        TextRenderer tr = c.textRenderer;
        int y = 4;
        if (ModConfig.fastMine) y = line(ctx, tr, "Minage x" + ModConfig.fmt(ModConfig.miningMultiplier), 0x55FF55, y);
        if (ModConfig.speedHack) y = line(ctx, tr, "Vitesse x" + ModConfig.fmt(ModConfig.speedMultiplier), 0x55FFFF, y);
        if (ModConfig.fly) y = line(ctx, tr, "Fly x" + ModConfig.fmt(ModConfig.flyMultiplier), 0xFFFF55, y);
        if (ModConfig.autoMine) y = line(ctx, tr, "Minage auto", 0xFFAA00, y);
        if (NoClip.active) y = line(ctx, tr, "NOCLIP", 0xFF8855, y);
        if (Ghost.active) line(ctx, tr, "GHOST", 0xFF55FF, y);
    }

    private static int line(DrawContext ctx, TextRenderer tr, String text, int color, int y) {
        ctx.drawTextWithShadow(tr, text, 4, y, color);
        return y + 10;
    }
}
