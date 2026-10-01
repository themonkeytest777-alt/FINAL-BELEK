package com.turbominer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.util.math.Vec3d;

/**
 * NoClip : ton vrai corps (inventaire, clic gauche pour casser) traverse les blocs.
 * Tant que ta hitbox est dans un bloc, aucune position n'est envoyee au serveur (il te garde au dernier endroit libre),
 * et le minage fonctionne normalement dans la portee de ce point. Quand tu retrouves de l'espace libre
 * (par exemple dans le tunnel que tu viens de creuser), la position est de nouveau envoyee.
 */
public final class NoClip {
    public static boolean active = false;

    private static ClientPlayerEntity owner;

    private NoClip() {}

    public static void toggle() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        if (!active) {
            owner = p;
            active = true;
        } else {
            active = false;
            owner = null;
            p.noClip = false;
            p.setVelocity(Vec3d.ZERO);
            p.fallDistance = 0;
        }
    }

    public static void reset() {
        active = false;
        owner = null;
    }

    /** true = ne pas envoyer la position au serveur (hitbox dans un bloc). */
    public static boolean freeze(ClientPlayerEntity p) {
        return active && !p.getWorld().isSpaceEmpty(p, p.getBoundingBox());
    }

    public static void tick(ClientPlayerEntity p) {
        if (!active || Ghost.active) return;
        if (owner != p || p.isDead()) {
            reset();
            return;
        }
        p.noClip = true;
        p.fallDistance = 0;
        PlayerAbilities ab = p.getAbilities();
        ab.allowFlying = true;
        ab.flying = true;
        p.setVelocity(Ghost.steer(p, 0.3 * ModConfig.noclipSpeed));
    }
}
