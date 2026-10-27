package com.shifustrike.mixin;

import com.shifustrike.guard.DamageGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Colete do CS aplicado depois da armadura do Minecraft (que na partida está vazia). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"), cancellable = true)
    private void shifustrike$coleteDoCs(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(DamageGuard.INSTANCE.afterArmor((LivingEntity) (Object) this, source, cir.getReturnValueF()));
    }
}
