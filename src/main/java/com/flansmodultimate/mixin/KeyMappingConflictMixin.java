package com.flansmodultimate.mixin;

import com.flansmodultimate.client.input.KeyConflictFilter;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops the controls screen reporting conflicts that cannot happen.
 *
 * <p>
 * {@code same} is only ever called from {@code KeyBindsList} to colour a
 * conflicting row, so this is presentation only: no binding changes what it
 * does. Forge's own implementation falls back to a bare key comparison whenever
 * two conflict contexts do not conflict, which makes the flight axes look like
 * they clash with the driving controls and with vanilla walking, even though a
 * pilot can reach none of those.
 * </p>
 */
@Mixin(KeyMapping.class)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class KeyMappingConflictMixin
{
    @Inject(method = "same(Lnet/minecraft/client/KeyMapping;)Z", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateHideImpossibleConflicts(KeyMapping other, CallbackInfoReturnable<Boolean> callback)
    {
        if (KeyConflictFilter.cannotOverlap((KeyMapping) (Object) this, other))
            callback.setReturnValue(false);
    }
}
