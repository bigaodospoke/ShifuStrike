package com.shifustrike.mixin;

import com.shifustrike.guard.AreaGuard;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explosão no meio do mapa da partida (granada, TNT) não quebra blocos. Vale também para o CustomExplosion do PointBlank. */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow @Final private Level level;

    @Shadow public abstract Vec3 center();

    @Inject(method = "interactsWithBlocks", at = @At("HEAD"), cancellable = true)
    private void shifustrike$protegeMapa(CallbackInfoReturnable<Boolean> cir) {
        Vec3 c = this.center();
        if (AreaGuard.INSTANCE.isProtected(this.level, c.x, c.y, c.z)) cir.setReturnValue(false);
    }
}
