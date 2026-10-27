package com.shifustrike.mixin;

import com.mojang.brigadier.ParseResults;
import com.shifustrike.guard.CommandGuard;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Jogador da partida não usa /home, /tpa, /spawn... só os comandos da lista da config.
 * performCommand é o caminho do comando digitado no chat (e também do console, via performPrefixedCommand).
 */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void shifustrike$bloqueiaComando(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        if (!CommandGuard.INSTANCE.check(parse.getContext().getSource(), command)) ci.cancel();
    }
}
