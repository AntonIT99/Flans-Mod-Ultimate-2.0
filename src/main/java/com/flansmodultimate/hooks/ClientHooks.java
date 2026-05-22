package com.flansmodultimate.hooks;

import com.flansmodultimate.hooks.server.ClientGunHooksNoop;
import com.flansmodultimate.hooks.server.ClientPlayerHooksNoop;
import com.flansmodultimate.hooks.server.ClientRenderHooksNoop;
import com.flansmodultimate.hooks.server.ClientSoundHooksNoop;
import com.flansmodultimate.hooks.server.ClientTooltipHooksNoop;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.loading.FMLEnvironment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHooks
{
    public static final IClientPlayerHooks PLAYER = FMLEnvironment.dist.isClient() ? ClientHookFactories.createPlayerHooks() : new ClientPlayerHooksNoop();
    public static final IClientTooltipHooks TOOLTIPS = FMLEnvironment.dist.isClient() ? ClientHookFactories.createTooltipsHooks() : new ClientTooltipHooksNoop();
    public static final IClientGunHooks GUN = FMLEnvironment.dist.isClient() ? ClientHookFactories.createGunHooks() : new ClientGunHooksNoop();
    public static final IClientRenderHooks RENDER = FMLEnvironment.dist.isClient() ? ClientHookFactories.createRenderHooks() : new ClientRenderHooksNoop();
    public static final IClientSoundHooks SOUND = FMLEnvironment.dist.isClient() ? ClientHookFactories.createSoundHooks() : new ClientSoundHooksNoop();
}