package com.flansmodultimate.hooks;

import com.flansmodultimate.hooks.server.ClientGunHooksNoop;
import com.flansmodultimate.hooks.server.ClientPlayerHooksNoop;
import com.flansmodultimate.hooks.server.ClientRenderHooksNoop;
import com.flansmodultimate.hooks.server.ClientSoundHooksNoop;
import com.flansmodultimate.hooks.server.ClientTooltipHooksNoop;
import com.flansmodultimate.platform.PlatformEnvironment;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHooks
{
    public static final IClientPlayerHooks PLAYER = PlatformEnvironment.isClient() ? ClientHookFactories.createPlayerHooks() : new ClientPlayerHooksNoop();
    public static final IClientTooltipHooks TOOLTIPS = PlatformEnvironment.isClient() ? ClientHookFactories.createTooltipsHooks() : new ClientTooltipHooksNoop();
    public static final IClientGunHooks GUN = PlatformEnvironment.isClient() ? ClientHookFactories.createGunHooks() : new ClientGunHooksNoop();
    public static final IClientRenderHooks RENDER = PlatformEnvironment.isClient() ? ClientHookFactories.createRenderHooks() : new ClientRenderHooksNoop();
    public static final IClientSoundHooks SOUND = PlatformEnvironment.isClient() ? ClientHookFactories.createSoundHooks() : new ClientSoundHooksNoop();
}
