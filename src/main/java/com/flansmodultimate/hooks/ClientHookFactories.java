package com.flansmodultimate.hooks;

import com.flansmodultimate.hooks.client.ClientGunHooksImpl;
import com.flansmodultimate.hooks.client.ClientPlayerHooksImpl;
import com.flansmodultimate.hooks.client.ClientRenderHooksImpl;
import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHookFactories
{
    public static IClientPlayerHooks createInput()
    {
        return new ClientPlayerHooksImpl();
    }

    public static IClientTooltipHooks createTooltips()
    {
        return new ClientTooltipHooksImpl();
    }

    public static IClientGunHooks createGun()
    {
        return new ClientGunHooksImpl();
    }

    public static IClientRenderHooks createRender()
    {
        return new ClientRenderHooksImpl();
    }
}