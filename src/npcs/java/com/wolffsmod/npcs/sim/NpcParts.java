package com.wolffsmod.npcs.sim;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.parts.FlansPartBodies;
import com.flansmodultimate.api.parts.IPartBody;
import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.api.parts.PartState;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelKind;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.PartStatePacket;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * The part hitboxes of an NPC drawn as a driveable: the body Flan rounds and explosions hit it by, posed where its
 * model is drawn, with the NPC's own changes to the parts on top.
 *
 * <p>
 * The NPC keeps one pool of health, as any Custom NPC does, and the parts share it out: each part holds the share of
 * the NPC's maximum health that its hit points are of all parts' hit points together. A hit takes the damage the NPC
 * would have taken from the part it struck, and no more than that part has left, so shooting a track off never kills.
 * The NPC dies with its core. Healing rebuilds the parts, and damage that names no part goes to the core, as on a
 * driveable.
 * </p>
 */
public final class NpcParts
{
    public static final String NBT_KEY = "WolffsModPartState";
    private static final String CORE = "core";
    private static final int SYNC_INTERVAL = 5;

    @Nullable
    private IPartBody body;
    /** The model the body was built for, kept even when it offers no parts so that it is not asked every tick. */
    @Nullable
    private IContentType bodyType;
    private int appliedSettings;
    private boolean settingsApplied;
    /** Saved part state waiting for the body it belongs to. */
    @Nullable
    private CompoundTag pendingState;
    /** Part state the server sent before the client had a body. */
    @Nullable
    private PartStatePacket pendingPacket;
    /** The part the hit being dealt struck; null for damage that names none. */
    @Nullable
    private String struck;
    private float healthBefore = Float.NaN;
    private boolean wasKilled;
    private NpcPartEffects effects = NpcPartEffects.NONE;
    private int sentSignature;
    private boolean sent;

    /** @return the body the NPC is hit by, while it has one */
    public Optional<IPartBody> body()
    {
        return Optional.ofNullable(body);
    }

    /** @return what the NPC has lost with its broken parts */
    public NpcPartEffects effects()
    {
        return effects;
    }

    // Tick

    /**
     * Runs once per tick on either side, after the NPC has moved and its turret has turned.
     *
     * @param model
     *            the content-pack model the NPC is drawn as, or null when it is drawn as none
     * @param hullYaw
     *            the yaw the model is drawn at
     * @param turret
     *            where the turret points, relative to the hull
     */
    public void tick(EntityNPCInterface npc, @Nullable FlanModelEntity model, float hullYaw, MountAim turret)
    {
        NpcWeaponSettings settings = (NpcWeaponSettings) npc.stats;
        NpcWeaponOptions options = settings.wolffsmodnpcsWeaponOptions();
        IContentType type = model == null ? null : model.getInfoType();
        if (model == null || type == null || !options.enabled(Feature.PART_HITBOXES))
        {
            drop();
            return;
        }
        if (type != bodyType)
        {
            drop();
            bodyType = type;
            body = FlansPartBodies.create(npc, type).orElse(null);
        }
        IPartBody current = body;
        if (current == null)
            return;
        NpcPartSettings parts = settings.wolffsmodnpcsParts();
        int signature = parts.signature();
        if (!settingsApplied || signature != appliedSettings)
        {
            parts.apply(current);
            appliedSettings = signature;
            settingsApplied = true;
        }
        // After the boxes: saved hit points are clamped to what a part can hold.
        if (pendingState != null)
        {
            current.loadState(pendingState);
            pendingState = null;
        }
        if (pendingPacket != null)
        {
            pendingPacket.applyTo(current);
            pendingPacket = null;
        }
        current.setBrokenPartsSolid(!options.enabled(Feature.BROKEN_PARTS_PASS));
        current.setOutlined(options.enabled(Feature.HITBOX_OUTLINES));
        current.setSolid(options.enabled(Feature.SOLID_HITBOXES));
        current.setPose(new PartBodyPose(NpcModels.world(npc, model, Vec3.ZERO, hullYaw), hullYaw, NpcModels.scale(npc), turret));
        if (!npc.level().isClientSide)
            serverTick(npc, current, options, FlanModelKind.of(type));
    }

    private void drop()
    {
        body = null;
        bodyType = null;
        settingsApplied = false;
        effects = NpcPartEffects.NONE;
        sent = false;
    }

    private void serverTick(EntityNPCInterface npc, IPartBody current, NpcWeaponOptions options, @Nullable FlanModelKind kind)
    {
        boolean killed = npc.isKilled();
        // A respawned NPC comes back whole.
        if (wasKilled && !killed)
            current.restore();
        wasKilled = killed;
        float share = share(npc, current);
        if (!options.enabled(Feature.PART_HEALTH) || share <= 0F)
        {
            // Without part health the parts are only hitboxes, and all of them stand.
            if (current.getHealth() < current.getMaxHealth())
                current.restore();
            effects = NpcPartEffects.NONE;
        }
        else if (!killed && npc.getHealth() > 0F)
            tickHealth(npc, current, share, options, kind);
        sync(npc, current);
    }

    /** Burns what is alight, follows health the NPC gained or lost by itself, and works out what its broken parts cost it. */
    private void tickHealth(EntityNPCInterface npc, IPartBody current, float share, NpcWeaponOptions options, @Nullable FlanModelKind kind)
    {
        if (current.tickFire() > 0F)
            npc.setHealth(coreBroken(current) ? 0F : current.getHealth() * share);
        reconcile(npc, current, share, options.enabled(Feature.PART_REPAIR));
        effects = options.enabled(Feature.PART_EFFECTS) && kind != null ? NpcPartEffects.of(current.getParts(), kind) : NpcPartEffects.NONE;
        if (effects.shotDown())
            npc.setHealth(0F);
    }

    /** @return NPC health per hit point of the body, or 0 when the body holds none */
    private static float share(EntityNPCInterface npc, IPartBody current)
    {
        float capacity = current.getMaxHealth();
        return capacity > 0F ? npc.getMaxHealth() / capacity : 0F;
    }

    private static boolean coreBroken(IPartBody current)
    {
        return current.getPart(CORE).filter(PartState::enabled).map(PartState::destroyed).orElse(false);
    }

    /**
     * Brings the parts in line with health the NPC gained or lost by other means than a hit: regeneration, a script, a
     * changed maximum.
     */
    private static void reconcile(EntityNPCInterface npc, IPartBody current, float share, boolean rebuild)
    {
        if (coreBroken(current))
        {
            npc.setHealth(0F);
            return;
        }
        float health = npc.getHealth();
        float held = current.getHealth() * share;
        float tolerance = Math.max(1.0E-3F, npc.getMaxHealth() * 1.0E-4F);
        if (health > held + tolerance)
        {
            repair(current, (health - held) / share, rebuild);
            // Parts that are not rebuilt leave their share of the health out of reach.
            float reached = current.getHealth() * share;
            if (reached < health - tolerance)
                npc.setHealth(reached);
        }
        else if (health < held - tolerance)
            shrink(current, held > 0F ? health / held : 0F);
    }

    /**
     * Shares hit points out among the damaged parts in proportion to what each is missing, so that a full heal makes
     * every part whole.
     *
     * @param rebuild
     *            whether broken parts take their share and come back
     */
    static void repair(IPartBody current, float amount, boolean rebuild)
    {
        List<PartState> parts = current.getParts().stream().filter(part -> part.enabled() && (rebuild || !part.destroyed())).toList();
        float missing = 0F;
        for (PartState part : parts)
            missing += Math.max(0F, part.box().health() - part.health());
        if (missing <= 0F || amount <= 0F)
            return;
        float filled = Math.min(1F, amount / missing);
        for (PartState part : parts)
        {
            float gap = part.box().health() - part.health();
            if (gap > 0F)
                current.setHealth(part.name(), part.health() + gap * filled);
        }
    }

    /** Takes the same share of its hit points from every standing part, which breaks none while any health is left. */
    static void shrink(IPartBody current, float kept)
    {
        float share = Math.max(0F, Math.min(1F, kept));
        for (PartState part : current.getParts())
        {
            if (part.enabled() && !part.destroyed() && part.health() > 0F)
                current.setHealth(part.name(), Math.max(Float.MIN_VALUE, part.health() * share));
        }
    }

    // Damage

    /**
     * Deals the damage of a Flan round or blast that struck one part, by the NPC's usual damage path, so that its
     * resistances, factions, scripts and minimum hits all still apply. Server side.
     */
    public boolean hurtPart(EntityNPCInterface npc, String part, DamageSource source, float amount)
    {
        struck = part;
        // Each part a blast reaches takes its own damage, and a driveable has no moment of grace between hits.
        npc.invulnerableTime = 0;
        try
        {
            return npc.hurt(source, amount);
        }
        finally
        {
            struck = null;
        }
    }

    /** Called as the NPC is about to lose health to a hit. */
    public void beforeDamage(EntityNPCInterface npc)
    {
        healthBefore = npc.getHealth();
    }

    /**
     * Called once the NPC has lost health to a hit, before it is checked for death: hands the loss to the part that was
     * struck, and gives back what that part could not take.
     */
    public void afterDamage(EntityNPCInterface npc, DamageSource source)
    {
        float before = healthBefore;
        healthBefore = Float.NaN;
        IPartBody current = body;
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        if (current == null || Float.isNaN(before) || !options.enabled(Feature.PART_HEALTH))
            return;
        float share = share(npc, current);
        float dealt = before - npc.getHealth();
        String part = struck == null ? CORE : struck;
        // A model without a core takes such damage as one pool; the parts follow on the next tick.
        if (share <= 0F || dealt <= 0F || current.getPart(part).filter(PartState::enabled).isEmpty())
            return;
        current.damage(part, dealt / share, source.is(DamageTypeTags.IS_FIRE));
        boolean fatal = coreBroken(current)
            || options.enabled(Feature.PART_EFFECTS) && bodyType != null && FlanModelKind.of(bodyType) == FlanModelKind.PLANE && NpcPartEffects.of(current.getParts(), FlanModelKind.PLANE).shotDown();
        npc.setHealth(fatal ? 0F : Math.min(before, current.getHealth() * share));
    }

    // Persistence and synchronisation

    /** @return the hit points of the parts, or null when the NPC has none to save */
    @Nullable
    public CompoundTag save()
    {
        if (body != null)
            return body.saveState();
        return pendingState == null ? null : pendingState.copy();
    }

    public void load(CompoundTag state)
    {
        pendingState = state.copy();
    }

    private void sync(EntityNPCInterface npc, IPartBody current)
    {
        int signature = current.signature();
        if (sent && signature == sentSignature || npc.tickCount % SYNC_INTERVAL != 0)
            return;
        sent = true;
        sentSignature = signature;
        NpcNetwork.sendToTracking(npc, PartStatePacket.of(npc.getId(), current));
    }

    /** @return the part state as sent to a client that starts tracking the NPC, while it has a body */
    public Optional<PartStatePacket> packet(EntityNPCInterface npc)
    {
        return body().map(current -> PartStatePacket.of(npc.getId(), current));
    }

    /** Takes the server's part state. Client only. */
    public void receive(PartStatePacket packet)
    {
        if (body != null)
            packet.applyTo(body);
        else
            pendingPacket = packet;
    }
}
