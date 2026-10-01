package com.turbominer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Mode ghost : le joueur traverse les blocs (noclip) et vole librement dans la direction du regard.
 * Aucun paquet de mouvement n'est envoye au serveur pendant le ghost (voir ClientPlayerEntityMixin).
 * Au 2e appui tu restes EXACTEMENT la ou tu es : le jeu envoie alors ta nouvelle position au serveur
 * (en solo elle est appliquee directement ; en multijoueur le serveur peut la refuser si elle est trop loin).
 */
public final class Ghost {
    public static boolean active = false;

    private static ClientPlayerEntity owner;

    private Ghost() {}

    public static void toggle() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        if (!active) {
            owner = p;
            active = true;
        } else {
            exit(p);
        }
    }

    private static void exit(ClientPlayerEntity p) {
        active = false;
        owner = null;
        p.noClip = false;
        p.setVelocity(Vec3d.ZERO);
        p.fallDistance = 0;
        syncIntegratedServer(p);
    }

    /** En solo (serveur integre) : on place directement le joueur serveur a la nouvelle position. */
    private static void syncIntegratedServer(ClientPlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isIntegratedServerRunning() || mc.getServer() == null) return;
        final double x = p.getX(), y = p.getY(), z = p.getZ();
        final float yaw = p.getYaw(), pitch = p.getPitch();
        final UUID id = p.getUuid();
        mc.getServer().execute(() -> {
            ServerPlayerEntity sp = mc.getServer().getPlayerManager().getPlayer(id);
            if (sp != null) sp.networkHandler.requestTeleport(x, y, z, yaw, pitch);
        });
    }

    public static void reset() {
        active = false;
        owner = null;
    }

    /** Vitesse de deplacement libre (dans la direction du regard) a partir des touches. */
    public static Vec3d steer(ClientPlayerEntity p, double speed) {
        Input in = p.input;
        double f = (in.pressingForward ? 1 : 0) - (in.pressingBack ? 1 : 0);
        double s = (in.pressingLeft ? 1 : 0) - (in.pressingRight ? 1 : 0);
        double vy = (in.jumping ? 1 : 0) - (in.sneaking ? 1 : 0);

        Vec3d look = p.getRotationVec(1.0F);
        double yaw = Math.toRadians(p.getYaw());
        Vec3d left = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));

        Vec3d v = look.multiply(f).add(left.multiply(s)).add(0, vy, 0);
        if (v.lengthSquared() < 1.0E-6) return Vec3d.ZERO;
        if (v.lengthSquared() > 1.0) v = v.normalize();
        return v.multiply(speed);
    }

    /** Appele a la fin de chaque tick client. */
    public static void tick(ClientPlayerEntity p) {
        if (!active) return;
        if (owner != p || p.isDead()) {
            reset();
            return;
        }

        p.noClip = true;
        p.fallDistance = 0;
        PlayerAbilities ab = p.getAbilities();
        ab.allowFlying = true;
        ab.flying = true;
        p.setVelocity(steer(p, 0.55 * ModConfig.flyMultiplier));
    }
}
