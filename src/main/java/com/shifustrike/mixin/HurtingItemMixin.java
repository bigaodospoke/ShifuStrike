package com.shifustrike.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shifustrike.pointblank.HeadshotFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Guarda se o tiro do PointBlank foi na cabeça, logo antes do dano. Seletores só por nome (sem descritor),
 * então não dependem de remapeamento; require = 0: se o PointBlank mudar, só o headshot deixa de ser detectado.
 */
@Pseudo
@Mixin(targets = "com.vicmatskiv.pointblank.item.HurtingItem", remap = false)
public abstract class HurtingItemMixin {
    @WrapOperation(
        method = "hurtEntity",
        at = @At(value = "INVOKE", target = "Lcom/vicmatskiv/pointblank/util/HitScan;isHeadshot", remap = false),
        remap = false,
        require = 0
    )
    private boolean shifustrike$guardaHeadshot(LivingEntity entity, Vec3 hit, Operation<Boolean> original) {
        boolean headshot = original.call(entity, hit);
        HeadshotFlag.INSTANCE.set(headshot);
        return headshot;
    }
}
