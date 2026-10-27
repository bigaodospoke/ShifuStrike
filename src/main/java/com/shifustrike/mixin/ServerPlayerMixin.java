package com.shifustrike.mixin;

import com.shifustrike.match.MatchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adversários vivos da mesma partida podem se ferir mesmo com o PvP do servidor desligado. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "canHarmPlayer", at = @At("HEAD"), cancellable = true)
    private void shifustrike$pvpDaPartida(Player other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof ServerPlayer o && MatchManager.INSTANCE.areOpponents((ServerPlayer) (Object) this, o)) {
            cir.setReturnValue(true);
        }
    }
}
