package com.flansmodultimate.hooks;

import com.flansmodultimate.hooks.client.ClientGunHooksImpl;
import com.flansmodultimate.hooks.client.ClientInputHooksImpl;
import com.flansmodultimate.hooks.client.ClientRenderHooksImpl;
import com.flansmodultimate.hooks.client.ClientTooltipHooksImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHookFactories
{
    public static IClientInputHooks createInput()
    {
        return new ClientInputHooksImpl();
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