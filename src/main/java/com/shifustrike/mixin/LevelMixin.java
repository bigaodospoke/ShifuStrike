package com.shifustrike.mixin;

import com.shifustrike.guard.AreaGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nada quebra bloco dentro do mapa da partida (bala do PointBlank no vidro, por exemplo). */
@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at = @At("HEAD"), cancellable = true)
    private void shifustrike$protegeBloco(BlockPos pos, boolean drop, Entity entity, int recursion, CallbackInfoReturnable<Boolean> cir) {
        if (AreaGuard.INSTANCE.isProtected((Level) (Object) this, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) cir.setReturnValue(false);
    }
}
