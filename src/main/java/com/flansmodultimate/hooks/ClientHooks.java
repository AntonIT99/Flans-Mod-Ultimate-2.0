package com.flansmodultimate.hooks;

import com.flansmodultimate.hooks.server.ClientGunHooksNoop;
import com.flansmodultimate.hooks.server.ClientPlayerHooksNoop;
import com.flansmodultimate.hooks.server.ClientRenderHooksNoop;
import com.flansmodultimate.hooks.server.ClientTooltipHooksNoop;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.fml.DistExecutor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHooks
{
    public static final IClientPlayerHooks PLAYER = DistExecutor.safeRunForDist(() -> ClientHookFactories::createInput, () -> ClientPlayerHooksNoop::new);
    public static final IClientTooltipHooks TOOLTIPS = DistExecutor.safeRunForDist(() -> ClientHookFactories::createTooltips, () -> ClientTooltipHooksNoop::new);
    public static final IClientGunHooks GUN = DistExecutor.safeRunForDist(() -> ClientHookFactories::createGun, () -> ClientGunHooksNoop::new);
    public static final IClientRenderHooks RENDER = DistExecutor.safeRunForDist(() -> ClientHookFactories::createRender, () -> ClientRenderHooksNoop::new);
}
