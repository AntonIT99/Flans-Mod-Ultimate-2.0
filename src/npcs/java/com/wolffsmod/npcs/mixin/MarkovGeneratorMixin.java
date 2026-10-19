package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.names.SoldierNames;
import nikedemos.markovnames.generators.MarkovGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Appends whole-name soldier lists to the generator entrypoint used by NPCs and scripts. */
@SuppressWarnings("java:S1118") // Mixin class; its static injector is merged into the target, not instantiated.
@Mixin(value = MarkovGenerator.class, remap = false)
public abstract class MarkovGeneratorMixin
{
    @Inject(method = "fetch(II)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private static void wolffsmodnpcsGenerateSoldierName(int generator, int gender, CallbackInfoReturnable<String> callback)
    {
        if (SoldierNames.contains(generator))
            callback.setReturnValue(SoldierNames.generate(generator, gender));
    }
}
