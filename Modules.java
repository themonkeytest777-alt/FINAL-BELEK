package com.turbominer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import com.turbominer.mixin.ClientPlayerInteractionManagerAccessor;

public final class Modules {
    private static boolean flyApplied = false;

    private Modules() {}

    // ---------- toggles ----------

    public static void toggleFastMine() {
        ModConfig.fastMine = !ModConfig.fastMine;
        ModConfig.save();
        notify("Minage rapide", ModConfig.fastMine);
    }

    public static void toggleSpeed() {
        ModConfig.speedHack = !ModConfig.speedHack;
        ModConfig.save();
        notify("Vitesse", ModConfig.speedHack);
    }

    public static void toggleFly() {
        ModConfig.fly = !ModConfig.fly;
        ModConfig.save();
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (ModConfig.fly && p != null && p.isOnGround()) {
            Vec3d v = p.getVelocity();
            p.setVelocity(v.x, 0.3, v.z); // petit saut pour decoller
        }
        notify("Fly", ModConfig.fly);
    }

    public static void toggleAutoMine() {
        ModConfig.autoMine = !ModConfig.autoMine;
        if (!ModConfig.autoMine) {
            MinecraftClient.getInstance().options.attackKey.setPressed(false);
        }
        notify("Minage auto", ModConfig.autoMine);
    }

    public static void toggleNoClip() {
        NoClip.toggle();
        notify("NoClip", NoClip.active);
    }

    public static void toggleGhost() {
        Ghost.toggle();
        notify("Ghost", Ghost.active);
    }

    private static void notify(String name, boolean on) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null) {
            p.sendMessage(Text.literal(name + " : " + (on ? "ON" : "OFF")), true);
        }
    }

    // ---------- ticks ----------

    public static void startTick(MinecraftClient c) {
        if (c.player == null) return;
        // Maintient le clic gauche : le jeu mine/attaque tout seul pendant le handleInputEvents du tick
        if (ModConfig.autoMine && c.currentScreen == null) {
            c.options.attackKey.setPressed(true);
        }
    }

    public static void endTick(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        if (p == null || c.world == null) {
            Ghost.reset();
            NoClip.reset();
            BiomeCompass.reset();
            flyApplied = false;
            return;
        }

        // Supprime le delai entre deux blocs
        if (ModConfig.fastMine && ModConfig.noMineDelay && c.interactionManager != null) {
            ((ClientPlayerInteractionManagerAccessor) c.interactionManager).turbominer$setBlockBreakingCooldown(0);
        }

        applyFly(p);
        Ghost.tick(p);
        NoClip.tick(p);
        applySpeed(p);
        BiomeCompass.tick(c);
    }

    private static void applyFly(ClientPlayerEntity p) {
        PlayerAbilities ab = p.getAbilities();
        boolean want = ModConfig.fly || Ghost.active || NoClip.active;
        if (want) {
            if (!flyApplied) {
                ab.allowFlying = true;
                ab.flying = true;
                flyApplied = true;
            }
            ab.allowFlying = true;
            ab.setFlySpeed(Ghost.active ? 0.05F : NoClip.active ? 0.0F : (float) (0.05 * ModConfig.flyMultiplier));
        } else if (flyApplied) {
            flyApplied = false;
            ab.setFlySpeed(0.05F);
            if (!p.isCreative() && !p.isSpectator()) {
                ab.allowFlying = false;
                ab.flying = false;
            }
        }
    }

    private static void applySpeed(ClientPlayerEntity p) {
        if (!ModConfig.speedHack || Ghost.active || NoClip.active) return;
        if (p.getAbilities().flying || p.hasVehicle() || p.isFallFlying()) return;

        Input in = p.input;
        double f = in.movementForward;
        double s = in.movementSideways;
        double len = Math.sqrt(f * f + s * s);
        if (len < 1.0E-4) return;
        if (len > 1.0) {
            f /= len;
            s /= len;
        }

        float yaw = p.getYaw() * 0.017453292F;
        double sin = MathHelper.sin(yaw);
        double cos = MathHelper.cos(yaw);
        double speed = 0.2806 * ModConfig.speedMultiplier; // 0.28 = vitesse de sprint (blocs/tick)

        Vec3d v = p.getVelocity();
        p.setVelocity((s * cos - f * sin) * speed, v.y, (f * cos + s * sin) * speed);
    }
}
