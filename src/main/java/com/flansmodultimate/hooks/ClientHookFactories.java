package com.flansmodultimate.hooks;

import com.flansmodultimate.hooks.client.ClientGunHooksImpl;
import com.flansmodultimate.hooks.client.ClientPlayerHooksImpl;
import com.flansmodultimate.hooks.client.ClientRenderHooksImpl;
import com.flansmodultimate.hooks.client.ClientSoundHooksImpl;
import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHookFactories
{
    public static IClientPlayerHooks createPlayerHooks()
    {
        return new ClientPlayerHooksImpl();
    }

    public static IClientTooltipHooks createTooltipsHooks()
    {
        return new ClientTooltipHooksImpl();
    }

    public static IClientGunHooks createGunHooks()
    {
        return new ClientGunHooksImpl();
    }

    public static IClientRenderHooks createRenderHooks()
    {
        return new ClientRenderHooksImpl();
    }

    public static IClientSoundHooks createSoundHooks()
    {
        return new ClientSoundHooksImpl();
    }
}