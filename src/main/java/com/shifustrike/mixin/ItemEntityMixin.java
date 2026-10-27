package com.shifustrike.mixin;

import com.shifustrike.guard.InteractionGuard;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Regras de pegar item do chão na partida (C4 só TR, uma primária e uma pistola por vez). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void shifustrike$regrasDePegar(Player player, CallbackInfo ci) {
        if (!InteractionGuard.INSTANCE.allowPickup(player, ((ItemEntity) (Object) this).getItem())) ci.cancel();
    }
}
