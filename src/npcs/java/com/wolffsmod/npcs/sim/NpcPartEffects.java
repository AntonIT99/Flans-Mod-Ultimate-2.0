package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.PartState;
import com.wolffsmod.npcs.model.FlanModelKind;

import java.util.*;

/**
 * What an NPC loses with the parts of its model that have been broken, following what a driveable loses with them.
 *
 * @param mobility
 *            share of its speed the NPC keeps: half on one track, none without tracks or hips, less with wheels,
 *            engine rooms, boilers, bow or stern gone
 * @param gunDisabled
 *            whether the main gun is out of action: its turret or barrel is gone, or both arms of a mecha
 * @param shotDown
 *            whether an aircraft has lost what keeps it in the air: a wing, its tail, its rotor or its airframe
 */
public record NpcPartEffects(float mobility, boolean gunDisabled, boolean shotDown)
{
    public static final NpcPartEffects NONE = new NpcPartEffects(1F, false, false);
    private static final List<String> WHEELS = List.of("corewheel", "frontwheel", "backwheel", "frontleftwheel", "frontrightwheel", "backleftwheel", "backrightwheel");
    private static final List<String> TRACKS = List.of("lefttrack", "righttrack");
    private static final List<String> ENGINES = List.of("engineroom1", "engineroom2", "engineroom3", "engineroom4", "engineroom5", "engineroom6", "engineroom7", "engineroom8", "engine", "engine2",
        "engine3", "engine4", "engine5", "engine6");
    private static final List<String> BOILERS = List.of("boilerroom1", "boilerroom2", "boilerroom3", "boilerroom4", "boilerroom5", "boilerroom6", "boilerroom7", "boilerroom8");
    private static final List<String> FLIGHT = List.of("leftwing", "rightwing", "tail", "blades", "airframe");

    /**
     * @param parts
     *            the parts of the NPC's body as they stand
     * @param kind
     *            the kind of model the NPC is
     */
    public static NpcPartEffects of(Collection<PartState> parts, FlanModelKind kind)
    {
        // Only a part that holds hit points can be broken; true once it has been.
        Map<String, Boolean> broken = new HashMap<>();
        for (PartState part : parts)
        {
            if (part.enabled() && part.box().health() > 0F)
                broken.put(part.name().toLowerCase(Locale.ROOT), part.destroyed());
        }
        boolean gun = isBroken(broken, "turret") || isBroken(broken, "barrel") || kind == FlanModelKind.MECHA && isBroken(broken, "leftarm") && isBroken(broken, "rightarm");
        boolean shotDown = kind == FlanModelKind.PLANE && FLIGHT.stream().anyMatch(name -> isBroken(broken, name));
        return new NpcPartEffects(mobility(broken, kind), gun, shotDown);
    }

    private static float mobility(Map<String, Boolean> broken, FlanModelKind kind)
    {
        if (kind == FlanModelKind.MECHA && isBroken(broken, "hips"))
            return 0F;
        float mobility = 1F;
        if (kind == FlanModelKind.VEHICLE)
        {
            float tracks = brokenShare(broken, TRACKS);
            if (tracks > 0F)
                mobility *= tracks >= 1F ? 0F : 0.5F;
            mobility *= 1F - brokenShare(broken, WHEELS);
        }
        // The penalty a driveable's throttle takes for its engine rooms, boilers, bow and stern.
        float engines = Math.max(brokenShare(broken, ENGINES), brokenShare(broken, BOILERS)) * 0.8F + (isBroken(broken, "stern") ? 0.1F : 0F) + (isBroken(broken, "bow") ? 0.1F : 0F);
        return mobility * (1F - Math.min(1F, engines));
    }

    private static boolean isBroken(Map<String, Boolean> broken, String part)
    {
        return Boolean.TRUE.equals(broken.get(part));
    }

    /** @return the share of the listed parts the model has that are broken; 0 when it has none of them */
    private static float brokenShare(Map<String, Boolean> broken, List<String> names)
    {
        int defined = 0;
        int gone = 0;
        for (String name : names)
        {
            Boolean state = broken.get(name);
            if (state == null)
                continue;
            defined++;
            if (Boolean.TRUE.equals(state))
                gone++;
        }
        return defined == 0 ? 0F : (float) gone / defined;
    }
}
