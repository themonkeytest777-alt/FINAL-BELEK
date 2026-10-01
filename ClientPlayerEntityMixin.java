package com.turbominer.mixin;

import com.turbominer.Ghost;
import com.turbominer.NoClip;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    /** PlayerEntity.tick remet noClip a false chaque tick : on le force avant le mouvement. */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void turbominer$forceNoClip(CallbackInfo ci) {
        if (Ghost.active || NoClip.active) {
            ((Entity) (Object) this).noClip = true;
        }
    }

    /** Ghost : aucun paquet de position. NoClip : pas de paquet tant que la hitbox est dans un bloc. */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void turbominer$freezeServerPosition(CallbackInfo ci) {
        if (Ghost.active || NoClip.freeze((ClientPlayerEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
