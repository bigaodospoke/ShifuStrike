package com.shifustrike.mixin;

import com.shifustrike.guard.CommandGuard;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jogador da partida não usa /home, /tpa, /spawn... só os comandos da lista da config. */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Inject(method = "performPrefixedCommand", at = @At("HEAD"), cancellable = true)
    private void shifustrike$bloqueiaComando(CommandSourceStack source, String command, CallbackInfo ci) {
        if (!CommandGuard.INSTANCE.check(source, command)) ci.cancel();
    }
}
