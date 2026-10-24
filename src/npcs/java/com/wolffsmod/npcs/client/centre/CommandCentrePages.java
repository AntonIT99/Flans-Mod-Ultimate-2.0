package com.wolffsmod.npcs.client.centre;

import com.flansmodultimate.api.client.commandcentre.FlansCommandCentreClient;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Adds the module's pages to the Teams command centre: factions, skirmishes, wars and the bots of the rotation. Client only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CommandCentrePages
{
    public static void register()
    {
        FlansCommandCentreClient.addPage(new FactionsPage());
        FlansCommandCentreClient.addPage(new SkirmishesPage());
        FlansCommandCentreClient.addPage(new WarsPage());
        FlansCommandCentreClient.addPage(new BotsPage());
    }
}
